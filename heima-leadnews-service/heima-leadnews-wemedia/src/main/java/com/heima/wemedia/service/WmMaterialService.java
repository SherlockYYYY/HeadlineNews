package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import org.springframework.web.multipart.MultipartFile;

public interface WmMaterialService extends IService<WmMaterial> {
    /*
    素材上传
     */
    public ResponseResult uploadPicture(MultipartFile multipartFile);

    /*
    素材列表
     */
    public ResponseResult findList(WmMaterialDto dto);
}