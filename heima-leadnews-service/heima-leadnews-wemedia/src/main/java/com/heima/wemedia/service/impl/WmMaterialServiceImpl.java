package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.service.WmMaterialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;


@Slf4j
@Service
@Transactional
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {
    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public ResponseResult uploadPicture(MultipartFile multipartFile) {
        //1.检查参数
        if(multipartFile == null || multipartFile.getSize() == 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2，上传图片到minio
        String fileName = UUID.randomUUID().toString().replace("-", "");
        String originalFilename = multipartFile.getOriginalFilename();
        String postfix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileId = null;
        try {
            fileId = fileStorageService.uploadImgFile("", fileName + postfix, multipartFile.getInputStream());
            log.info("上传图片成功到minio，文件id为：{}",fileId);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("WmMaterialServiceImpl上传文件失败");
            return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR);
        }
        //3.保存图片信息到数据库
        WmMaterial wmMaterial = new WmMaterial();
        wmMaterial.setUserId(WmThreadLocalUtil.getUser().getId());
        wmMaterial.setUrl(fileId);
        wmMaterial.setIsCollection(Short.valueOf("0"));  //默认不收藏
        wmMaterial.setType(Short.valueOf("0"));  //0图片 1视频
        wmMaterial.setCreatedTime(new Date());
        save(wmMaterial);
        //4.返回结果
        return ResponseResult.okResult(wmMaterial);
    }

    @Override
    public ResponseResult findList(WmMaterialDto dto) {
        //1.检查参数
        dto.checkParam();
        //2.分页查询
        IPage pages  = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<WmMaterial> queryWrapper = new LambdaQueryWrapper<>();
        //是否收藏
        if(dto.getIsCollection() != null && dto.getIsCollection() == 1){
            queryWrapper.eq(WmMaterial::getIsCollection,dto.getIsCollection());
        }
        //按照用户查询
        queryWrapper.eq(WmMaterial::getUserId,WmThreadLocalUtil.getUser().getId());
        //按照时间倒叙
        queryWrapper.orderByDesc(WmMaterial::getCreatedTime);
        //3.结果查询
        pages = page(pages,queryWrapper);
        ResponseResult responseResult = new PageResponseResult(dto.getPage(),dto.getSize(),(int)pages.getTotal());
        responseResult.setData(pages.getRecords());
        return responseResult;
    }

    @Override
    public ResponseResult delPicture(Integer id) {
        //1.检查参数
        if(id == null){
            log.error("{} WmMaterialServiceImpl删除图片失败，id为null",501);
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.根据id查询图片
        WmMaterial wmMaterial = getById(id);
        if(wmMaterial == null){
            log.error("{} WmMaterialServiceImpl删除图片失败，图片不存在",1002);
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        if(!Objects.equals(wmMaterial.getUserId(), WmThreadLocalUtil.getUser().getId())){
            log.error("{} WmMaterialServiceImpl删除图片失败，没有权限",3000);
            return ResponseResult.errorResult(AppHttpCodeEnum.NO_OPERATOR_AUTH);
        }
        //3.删除图片
        boolean flag = removeById(id);
        if(flag){
            log.info("{} WmMaterialServiceImpl删除图片成功",200);
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }
        log.error("{} WmMaterialServiceImpl删除图片失败",501);
        return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"图片删除失败");
    }

    @Override
    public ResponseResult collect(Integer id) {
        //1.检查参数
        if(id == null){
            log.error("{} WmMaterialServiceImpl收藏图片失败，id为null",501);
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmMaterial wmMaterial = getById(id);
        if(wmMaterial == null){
            log.error("{} WmMaterialServiceImpl收藏图片失败，图片不存在",1002);
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        if(!Objects.equals(wmMaterial.getUserId(), WmThreadLocalUtil.getUser().getId())){
            log.error("{} WmMaterialServiceImpl收藏图片失败，没有权限",3000);
            return ResponseResult.errorResult(AppHttpCodeEnum.NO_OPERATOR_AUTH);
        }
        wmMaterial.setIsCollection((short)1);
        boolean flag = updateById(wmMaterial);
        if(flag){
            log.info("{} WmMaterialServiceImpl收藏图片成功",200);
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }
        log.error("{} WmMaterialServiceImpl收藏图片失败",501);
        wmMaterial.setIsCollection((short)0);
        return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"图片收藏失败");
    }

    @Override
    public ResponseResult cancelCollect(Integer id) {
        //1.检查参数
        if(id == null){
            log.error("{} WmMaterialServiceImpl取消收藏图片失败，id为null",501);
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.根据id查询图片
        WmMaterial wmMaterial = getById(id);
        if(wmMaterial == null){
            log.error("{} WmMaterialServiceImpl取消收藏图片失败，图片不存在",1002);
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        if(!Objects.equals(wmMaterial.getUserId(), WmThreadLocalUtil.getUser().getId())){
            log.error("{} WmMaterialServiceImpl取消收藏图片失败，没有权限",3000);
            return ResponseResult.errorResult(AppHttpCodeEnum.NO_OPERATOR_AUTH);
        }
        wmMaterial.setIsCollection((short)0);
        boolean flag = updateById(wmMaterial);
        if(flag){
            log.info("{} WmMaterialServiceImpl取消收藏图片成功",200);
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }
        log.error("{} WmMaterialServiceImpl取消收藏图片失败",501);
        wmMaterial.setIsCollection((short)1);
        return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"图片取消收藏失败");
    }
}
