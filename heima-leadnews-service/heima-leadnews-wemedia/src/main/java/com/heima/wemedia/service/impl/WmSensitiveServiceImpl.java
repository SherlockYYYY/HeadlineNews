package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.SensitiveDto;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.service.WmSensitiveService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
@Slf4j
public class WmSensitiveServiceImpl extends ServiceImpl<WmSensitiveMapper, WmSensitive> implements WmSensitiveService {
    /**
     * 查询
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult list(SensitiveDto dto) {
        if(dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        dto.checkParam(); //校验分页查询的参数

        //2.模糊查询+分页
        IPage pageQuery = new Page(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<WmSensitive> queryWrapper = new LambdaQueryWrapper<>();
        if(StringUtils.isNotBlank(dto.getName())){
            queryWrapper.like(WmSensitive::getSensitives,dto.getName());
        }
        pageQuery = page(pageQuery, queryWrapper);

        //3.结果返回
        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) pageQuery.getTotal());
        responseResult.setData(pageQuery.getRecords());
        return responseResult;
    }

    /**
     * 新增
     *
     * @param wmSensitive
     * @return
     */
    @Override
    public ResponseResult insert(WmSensitive wmSensitive) {
        if(wmSensitive == null || StringUtils.isBlank(wmSensitive.getSensitives())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //存在的敏感词不能保存
        WmSensitive sensitive = getOne(Wrappers.<WmSensitive>lambdaQuery().eq(WmSensitive::getSensitives,wmSensitive.getSensitives()));
        if(sensitive != null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"敏感词已存在");
        }
        //2.保存
        wmSensitive.setCreatedTime(new Date());
        save(wmSensitive);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 修改
     *
     * @param wmSensitive
     * @return
     */
    @Override
    public ResponseResult update(WmSensitive wmSensitive) {
        if(wmSensitive == null || wmSensitive.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        long count = count(Wrappers.<WmSensitive>lambdaQuery().eq(WmSensitive::getSensitives,wmSensitive.getSensitives()));
        if (count > 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"敏感词已存在");
        }
        //2.修改
        updateById(wmSensitive);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 删除
     *
     * @param id
     * @return
     */
    @Override
    public ResponseResult delete(Integer id) {
        if (id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.查询敏感词
        WmSensitive wmSensitive = getById(id);
        if(wmSensitive == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        //3.删除
        removeById(id);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
