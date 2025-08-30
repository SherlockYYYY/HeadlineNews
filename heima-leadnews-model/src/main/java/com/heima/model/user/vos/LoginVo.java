package com.heima.model.user.vos;

import com.heima.model.user.pojos.ApUser;
import lombok.Data;
import java.io.Serializable;

/**
 * <p>
 * APP用户信息表
 * </p>
 *
 * @author itheima
 */
@Data ///去掉密码 和 盐
public class LoginVo implements Serializable {

    /**
     * JWT 令牌
     */
    private String token;

    /**
     * 用户信息 (已脱敏，不包含密码和盐)
     */
    private ApUser user;


    // 私有构造函数，强制使用静态工厂方法
    private LoginVo() {}

    /**
     * 静态工厂方法：从 ApUser 和 token 创建 LoginVo
     * 在此方法中完成敏感信息的过滤
     * @param token JWT token
     * @param apUser 原始用户对象
     * @return 脱敏后的 LoginVo
     */

    public static LoginVo of(String token, ApUser apUser) {
        LoginVo vo = new LoginVo();
        vo.setToken(token);

        // 创建一个新的 ApUser 对象，或复制原对象，然后清空敏感字段
        if (apUser != null) {
            ApUser safeUser = new ApUser();
            // 手动拷贝需要的字段
            safeUser.setId(apUser.getId());
            safeUser.setName(apUser.getName());
            safeUser.setPhone(apUser.getPhone());
            safeUser.setImage(apUser.getImage());
            safeUser.setSex(apUser.getSex());
            safeUser.setCertification(apUser.getCertification());
            safeUser.setIdentityAuthentication(apUser.getIdentityAuthentication());
            safeUser.setStatus(apUser.getStatus());
            safeUser.setFlag(apUser.getFlag());
            safeUser.setCreatedTime(apUser.getCreatedTime());
            // ... 其他需要返回给前端的非敏感字段

            //  不拷贝以下敏感字段
            // safeUser.setPassword(apUser.getPassword());
            // safeUser.setSalt(apUser.getSalt());
            vo.setUser(safeUser);
        }
        return vo;
    }

    /**
     * 便捷的游客登录创建方法
     * @param token 游客 Token
     * @return LoginVo
     */
    public static LoginVo ofGuest(String token) {
        LoginVo vo = new LoginVo();
        vo.setToken(token);
        vo.setUser(null); // 或者设置一个默认的游客用户对象
        return vo;
    }


}