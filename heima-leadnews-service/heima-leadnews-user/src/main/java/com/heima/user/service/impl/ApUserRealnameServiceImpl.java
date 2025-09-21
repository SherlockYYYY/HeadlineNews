package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.UserConstants;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.AuthDto;
import com.heima.apis.wemedia.IWemediaClient;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.user.pojos.ApUserRealname;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.mapper.ApUserRealnameMapper;
import com.heima.user.service.ApUserRealnameService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Slf4j
@Transactional
public class ApUserRealnameServiceImpl extends ServiceImpl<ApUserRealnameMapper, ApUserRealname> implements ApUserRealnameService {
    @Autowired
    private IWemediaClient wemediaClient;
    @Autowired
    private ApUserMapper apUserMapper;
    
    /**
     * 按照状态分页查询用户列表
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult loadListByStatus(AuthDto dto) {
        if (dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //分页条件查询
        dto.checkParam();

        //2.分页根据状态精确查询
        IPage pageQuery = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<ApUserRealname> lambdaQueryWrapper = new LambdaQueryWrapper();
        if(dto.getStatus() != null){
            lambdaQueryWrapper.eq(ApUserRealname::getStatus,dto.getStatus());  //有的审核通过，有的不通过
        }
        IPage page = page(pageQuery,lambdaQueryWrapper);

        //结果返回
        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }

    /**
     * @param dto
     * @param status 2审核失败    9审核成功
     * @return
     */
    @Override
    public ResponseResult updateStatus(AuthDto dto, Short status) {
        if (dto == null || dto.getId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        ApUserRealname aopUserRealname = new ApUserRealname();
        aopUserRealname.setId(dto.getId());
        aopUserRealname.setStatus(status);
        if(StringUtils.isNotBlank(dto.getMsg())){
            aopUserRealname.setReason(dto.getMsg());  //如果消息不为空  就设置原因
        }
        updateById(aopUserRealname);

        //如果状态为9  审核成功 为其创建自媒体账号
        if(status.equals(UserConstants.PASS_AUTH)){
            ResponseResult responseResult = createWmUserAndAuthor(dto);
            if (responseResult!= null){
                return responseResult;
            }

        }
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
    
    private ResponseResult createWmUserAndAuthor(AuthDto dto) {
        Integer userRealNameId = dto.getId();
        ApUserRealname apUserRealname = getById(userRealNameId);
        if (apUserRealname == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        //查询app端用户信息
        Integer userId = apUserRealname.getUserId();
        ApUser apUser = apUserMapper.selectById(userId);
        if (apUser == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        //前面是在user里查出实名数据 id 后面还有userid
        //这里是直接在ap_user表查出用户信息

        //创建自媒体用户
        WmUser wmUser = wemediaClient.findWmUserByName(apUser.getName());
        if (wmUser == null){
            wmUser = new WmUser();
            wmUser.setApUserId(apUser.getId());
            wmUser.setName(apUser.getName());
            wmUser.setPassword(apUser.getPassword());
            wmUser.setSalt(apUser.getSalt());
            wmUser.setPhone(apUser.getPhone());
            wmUser.setStatus(9);
            wmUser.setCreatedTime(new Date());
            wemediaClient.saveWmUser(wmUser);
        }
        apUser.setFlag((short)1);  //自媒体人
        apUserMapper.updateById(apUser);
        return null;
    }
}
