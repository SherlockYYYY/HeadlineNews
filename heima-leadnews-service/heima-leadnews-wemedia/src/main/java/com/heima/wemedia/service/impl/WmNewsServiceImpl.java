package com.heima.wemedia.service.impl;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.WemediaConstants;
import com.heima.common.constants.WmNewsMessageConstants;
import com.heima.common.exception.CustomException;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsService;
import com.heima.wemedia.service.WmNewsTaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {

    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;
    @Autowired
    private WmMaterialMapper wmMaterialMapper;

    @Autowired
    private WmNewsTaskService wmNewsTaskService;

    @Autowired
    private KafkaTemplate<String,  String> kafkaTemplate;

    /**
     * 条件查询查询文章
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult findList(WmNewsPageReqDto dto) {
        //1,检查参数
        //分页检查
        dto.checkParam();
        //分页条件查询
        IPage pgs = new Page(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<WmNews> wrapper = new LambdaQueryWrapper<>();
        //状态精确查询
        if (dto.getStatus() != null) {
            wrapper.eq(WmNews::getStatus, dto.getStatus());
        }
        //频道精确查询
        if (dto.getChannelId() != null) {
            wrapper.eq(WmNews::getChannelId, dto.getChannelId());
        }
        //时间范围查询
        if (dto.getBeginPubDate() != null && dto.getEndPubDate() != null) {
            wrapper.between(WmNews::getPublishTime, dto.getBeginPubDate(), dto.getEndPubDate());
        }
        //关键字查询
        if (StringUtils.isNotBlank(dto.getKeyword())) {
            wrapper.like(WmNews::getTitle, dto.getKeyword());
        }
        //查询当前登陆人的文章
        wrapper.eq(WmNews::getUserId, WmThreadLocalUtil.getUser().getId());
        //倒序
        wrapper.orderByDesc(WmNews::getPublishTime);
        pgs = page(pgs, wrapper);

        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) pgs.getTotal());
        responseResult.setData(pgs.getRecords());
        return responseResult;
    }

    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;

    @Override
    public ResponseResult submit(WmNewsDto dto) throws IOException {
        //调教判断
        if (dto == null || dto.getContent() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //1.保存或修改文章
        WmNews wmNews = new WmNews();
        //属性拷贝  属性名称和类型相同时才能拷贝
        BeanUtils.copyProperties(dto, wmNews);
        //封面图片  list --->  String
        if (dto.getImages() != null && dto.getImages().size() > 0) {
            String imageStr = StringUtils.join(dto.getImages(), ",");
            wmNews.setImages(imageStr);
        }
        //如果当前封面类型为自动  -1
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {   //因为数据库里type是无符号，不能是负数
            wmNews.setType(null);
        }

        saveOrUpdateWmNews(wmNews);
        //2.判断是否为草稿  如果是 就结束
        if (dto.getStatus().equals(WmNews.Status.NORMAL.getCode())) {
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);  //草稿不需要图片与素材之间的绑定关系
        }
        //3.保存文章内容图片与素材的关系
        //获取到文章内容中的图片信息
        List<String> materials = extractUrlInfo(dto.getContent());
        saveRelationForContent(materials, wmNews.getId());
        //4.保存文章封面图片与素材的关系  如果是自动 需要匹配封面图片
        saveRelationForCover(dto, wmNews, materials);
        //5.自媒体文章审核  异步线程审核
//        wmNewsAutoScanService.autoScan(wmNews.getId());
        wmNewsTaskService.addNewsToTask(wmNews.getId(), wmNews.getPublishTime());

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 保存文章封面图片与素材的关系  //如果是自动  就需要匹配封面图片
     * 规则 如果1.如果内容图片>=1 小于3 单图 type1
     * 2.如果内容图片>=3  多图 type3
     * 3.如果无图  单图 type0
     *
     * @param dto
     * @param wmNews
     * @param materials
     */
    private void saveRelationForCover(WmNewsDto dto, WmNews wmNews, List<String> materials) {
        List<String> images = dto.getImages();
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            if (materials.size() > 1 && materials.size() < 3) {
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
                images = materials.stream().limit(1).collect(Collectors.toList());
            } else if (materials.size() >= 3) {
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
                images = materials.stream().limit(3).collect(Collectors.toList());
            } else {
                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);

            }
            //修改文章
            if (images != null && images.size() > 0) {
                wmNews.setImages(StringUtils.join(images, ","));
            }
            updateById(wmNews);
        }
        if (images != null && images.size() > 0) {
            saveRelationInfo(images, wmNews.getId(), WemediaConstants.WM_COVER_REFERENCE);
        }
    }

    /**
     * 保存文章封面图片与素材的关系
     *
     * @param
     * @param id
     */
    private void saveRelationForContent(List<String> materials, Integer id) {
        saveRelationInfo(materials, id, WemediaConstants.WM_CONTENT_REFERENCE); ///0是内容引用 1是封面图片引用
    }

    /**
     * 保存内容或者封面文章图片与素材的关系
     *
     * @param
     * @param id
     */
    private void saveRelationInfo(List<String> materials, Integer id, Short wmContentReference) {
        if (materials != null && !materials.isEmpty()) {
            //通过图片的url 查询素材id
            List<WmMaterial> dbMaterials = wmMaterialMapper.selectList(new LambdaQueryWrapper<WmMaterial>().in(WmMaterial::getUrl, materials));

            //判断素材是否有效
            if (dbMaterials == null || dbMaterials.size() == 0) {
                //手动抛出异常 能提示调用者 进行数据回滚
                throw new CustomException(AppHttpCodeEnum.MATERIALS_REFERENCE_FAIL);
            }

            if (materials.size() != dbMaterials.size()) {
                throw new CustomException(AppHttpCodeEnum.MATERIALS_REFERENCE_FAIL);
            }

            List<Integer> materialIds = dbMaterials.stream().map(WmMaterial::getId).collect(Collectors.toList());
            wmNewsMaterialMapper.saveRelations(materialIds, id, wmContentReference);
        }

    }

    /**
     * 从文章内容中提取图片信息
     *
     * @param content
     * @return
     */
    private List<String> extractUrlInfo(String content) {
        List<String> materials = new ArrayList<>();
        List<Map> maps = JSON.parseArray(content, Map.class);
        for (Map map : maps) {
            if (map.get("type").equals("image")) {
                String imgUrl = (String) map.get("value");
                materials.add(imgUrl);
            }
        }
        return materials;
    }


    /**
     * 保存或修改文章
     *
     * @param wmNews
     */
    private void saveOrUpdateWmNews(WmNews wmNews) {
        //补全属性
        wmNews.setUserId(WmThreadLocalUtil.getUser().getId());
        wmNews.setCreatedTime(new Date());
        wmNews.setSubmitedTime(new Date());
        wmNews.setEnable((short) 1); //默认上架
        //保存或修改
        if (wmNews.getId() == null) {
            //保存
            save(wmNews);
        } else {

            //删除文章内容图片与素材的关系
            wmNewsMaterialMapper.delete(new LambdaQueryWrapper<WmNewsMaterial>().eq(WmNewsMaterial::getNewsId, wmNews.getId()));
            //修改
            updateById(wmNews);
        }
    }

    /**
     * 上下架
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult downOrUp(WmNewsDto dto) {
        //1.检查参数
        if (dto.getId() == null) {  //先判断前端传得参数是不是空  还没查文章
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2，查询文章
        WmNews wmNews = getById(dto.getId());  //到数据库里查文章 看有没有这个文章
        if (wmNews == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "文章不存在");
        }
        //3，判断文章是否已发布
        if (!wmNews.getStatus().equals(WmNews.Status.PUBLISHED.getCode())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "当前文章不是发布状态，不能进行上下架操作");
        }
        //4.修改文章enable  因为虽然9是发布状态，但是发布不等于上架，上下架使用enable字段判断
        //app端有个isdown字段判断是否上下架
        if (dto.getEnable() != null && dto.getEnable() > -1 && dto.getEnable() < 2) {
            //保证该字段的值在为 0 或者 1
            update(Wrappers.<WmNews>lambdaUpdate().set(WmNews::getEnable, dto.getEnable())
                    .eq(WmNews::getId, dto.getId()));
            //用kafka发送消息给app端 修改isdown字段下架文章
            if(wmNews.getArticleId() != null){
                Map<String, Object> data = new HashMap<>();
                data.put("articleId", wmNews.getArticleId());
                data.put("enable", dto.getEnable());
                kafkaTemplate.send(WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC, JSON.toJSONString( data));  //必须要发一个字符串，为了方便序列化 转成json形式字符串
            }


        }

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

}
