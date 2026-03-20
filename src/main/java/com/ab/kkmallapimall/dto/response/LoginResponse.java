package com.ab.kkmallapimall.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 令牌类型
     */
    private String tokenType = "Bearer";

    /**
     * 用户信息
     */
    private UserInfoVO userInfo;

    public LoginResponse(String accessToken, UserInfoVO userInfo) {
        this.accessToken = accessToken;
        this.userInfo = userInfo;
    }
}
