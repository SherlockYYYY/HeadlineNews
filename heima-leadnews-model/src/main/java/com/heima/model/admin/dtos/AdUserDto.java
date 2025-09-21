package com.heima.model.admin.dtos;

import lombok.Data;

@Data
public class AdUserDto {
    /**
     * 登录用户名
     */
    private String name;

    /**
     * 登录密码
     */
    private String password;
}
