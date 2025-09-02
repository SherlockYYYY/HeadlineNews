package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.heima.apis.article.IArticleClient;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {

    @Autowired
    private WmNewsMapper wmNewsMapper;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private IArticleClient articleClient;

    @Autowired
    private WmChannelMapper wmChannelMapper;

    @Autowired
    private WmUserMapper wmUserMapper;
    /**
     * 自媒体文章审核  但是没有集成阿里云接口 这里直接审核通过
     * @param id 自媒体文章id
     * @return
     */

    @Override
    public void autoScan(Integer id) {
        //1,查询自媒体文章
        WmNews wmNews = wmNewsMapper.selectById(id);
        if(wmNews == null){
            throw new RuntimeException("WmNewsAutoScanServiceImpl报错-自媒体文章不存在");
        }
        if(wmNews.getStatus().equals(WmNews.Status.SUBMIT.getCode())){
            //从内容中抽取文章内容和图片
            Map<String, Object> textAndImages = extractTextAndImages(wmNews);
            //2.审核内容
            boolean isTextPass = handleTextScan((String) textAndImages.get("content"),wmNews);
            if(!isTextPass) return;

            //3.审核图片
            boolean isImagePass = handleImageScan((List<String>) textAndImages.get("images"),wmNews);
            if(!isImagePass) return;
            //4.审核成功 保存app端相关文章数据
            ResponseResult responseResult = saveAppArticle(wmNews);
            if(!responseResult.getCode().equals(200)){
                throw new RuntimeException("WmNewsAutoScanServiceImpl报错-文章审核-保存app端相关文章数据失败");
            }

        }


    }
    /**
     * 保存app端相关文章数据
     * @param wmNews
     */
    private ResponseResult saveAppArticle(WmNews wmNews) {
        ArticleDto dto = new ArticleDto();
        //属性拷贝
        BeanUtils.copyProperties(wmNews,dto);
        //文章的布局
        dto.setLayout(wmNews.getType());
        //频道
        WmChannel wmChannel = wmChannelMapper.selectById(wmNews.getChannelId());
        if(wmChannel != null){
            dto.setChannelName(wmChannel.getName());
        }
        //作者

        dto.setAuthorId((long) wmNews.getUserId());
        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
        if(wmUser != null){
            dto.setAuthorName(wmUser.getName());
        }
        //设置文章id
        if(wmNews.getArticleId() != null){
            dto.setId(wmNews.getArticleId());
        }
        dto.setCreatedTime(new Date());
        //1.调用文章服务保存文章
        ResponseResult responseResult = articleClient.saveArticle(dto);
        wmNews.setStatus((short) 9);
        wmNews.setReason("审核成功");
        //回填article Id
        wmNews.setArticleId((Long)responseResult.getData());
        wmNewsMapper.updateById(wmNews);
        return responseResult;
    }

    /**
     * 审核图片
     * @param images
     * @param wmNews
     * @return
     */
    private boolean handleImageScan(List<String> images, WmNews wmNews) {
        boolean flag = true;
        //去重
//        images = images.stream().distinct().collect(Collectors.toList());
//        for (String image : images) {
//            //上传图片到fastdfs
//            byte[] bytes = fileStorageService.downLoadFile(image);
//        }

        return flag;

    }
    /**
     * 审核文本
     * @param content
     * @param wmNews
     * @return
     */

    private boolean handleTextScan(String content, WmNews wmNews) {
        boolean flag = true;

        return flag;
    }

    /**
     * 从内容中抽取文章内容和图片
     * 提取文章封面图片
     * @param wmNews
     * @return
     */
    private Map<String, Object> extractTextAndImages(WmNews wmNews) {
        //存储纯文本内容
        StringBuilder sb = new StringBuilder();

        List<String> images = new ArrayList<>();
        if(StringUtils.isNotBlank( wmNews.getContent())){
            List<Map> maps = JSONArray.parseArray(wmNews.getContent(), Map.class);
            for (Map map : maps) {
                if("text".equals(map.get("type"))){
                    sb.append(map.get("value"));
                }
                if("image".equals(map.get("type"))){
                    images.add((String) map.get("value"));
                }
            }
        }

        //提取文章的封面图片
        if(StringUtils.isNotBlank(wmNews.getImages())){
            String[] split = wmNews.getImages().split(",");
            images.addAll(Arrays.asList(split));
        }

        Map<String, Object> map = new HashMap<>();
        map.put("content",sb.toString());
        map.put("images",images);
        return map;

    }
}
