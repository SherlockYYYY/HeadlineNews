package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.ChannelDto;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.service.WmChannelService;
import com.heima.wemedia.service.WmNewsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional
@Slf4j
public class WmChannelServiceImpl extends ServiceImpl<WmChannelMapper, WmChannel> implements WmChannelService {

    @Autowired
    WmNewsService wmNewsService;
    /**
     * 查询所有频道
     * @return
     */
    @Override
    public ResponseResult findAll() {
        return ResponseResult.okResult(list());
    }

    @Override
    public ResponseResult findByNameAndPage(ChannelDto dto) {
        if(dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        dto.checkParam(); //把页和大小进行判断

        IPage queryPage  = new Page(dto.getPage(),dto.getSize());

        //根据名字模糊查询
        LambdaQueryWrapper<WmChannel> queryWrapper = new LambdaQueryWrapper<>();
        if(StringUtils.isNotBlank(dto.getName())){
            queryWrapper.like(WmChannel::getName,dto.getName()); ///name可以为空 空就是查询全部
        }

        queryPage = page(queryPage, queryWrapper);

        //3.结果返回

        ResponseResult responseResult = new PageResponseResult(dto.getPage(),dto.getSize(),(int)queryPage.getTotal());
        responseResult.setData(queryPage.getRecords());  //将记录放进去
        return responseResult;
    }

    /**
     * 保存
     *
     * @param wmChannel
     * @return
     */
    @Override
    public ResponseResult insert(WmChannel wmChannel) {
        if(wmChannel == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        WmChannel one = getOne(Wrappers.<WmChannel>lambdaQuery().eq(WmChannel::getName, wmChannel.getName()));
        if(one != null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST,"频道名称已存在");
        }

        wmChannel.setCreatedTime(new Date());
        wmChannel.setIsDefault( true);
        save(wmChannel);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 修改
     *
     * @param wmChannel
     * @return
     */
    @Override
    public ResponseResult update(WmChannel wmChannel) {
        if(wmChannel == null || wmChannel.getId() == null){ //没有参数 或者文章不存在
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //如果被引用就不能修改成禁用状态
        //查询频道id一样，并且是发布状态的文章
        LambdaQueryWrapper<WmNews> queryWrapper = Wrappers.<WmNews>lambdaQuery().eq(WmNews::getChannelId, wmChannel.getId()).eq(WmNews::getStatus, WmNews.Status.PUBLISHED.getCode());
        if(wmNewsService.count(queryWrapper) > 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST,"频道下有文章正在发布,不能禁用");
        }

        int count = count(Wrappers.<WmChannel>lambdaQuery().eq(WmChannel::getName, wmChannel.getName()));
        if (count > 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST,"频道名称已存在");
        }
        updateById(wmChannel);
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
        if(id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.查询频道
        WmChannel wmChannel = getById(id);
        if(wmChannel == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"频道不存在");
        }
        //3.频道是否有效
        if(wmChannel.getStatus()){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST,"频道是启用状态,不能删除");
        }
        //判断是否被引用
        int count = wmNewsService.count(Wrappers.<WmNews>lambdaQuery().eq(WmNews::getChannelId, wmChannel.getId())
                .eq(WmNews::getStatus, WmNews.Status.PUBLISHED.getCode()));
        if(count > 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"频道被引用不能删除");
        }
        //4.删除
        removeById(id);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}