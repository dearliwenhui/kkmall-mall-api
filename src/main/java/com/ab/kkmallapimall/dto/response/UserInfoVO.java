package com.ab.kkmallapimall.dto.response;

import lombok.Data;

import java.time.LocalDate;

/**
 * 用户信息VO
 */
@Data
public class UserInfoVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String phone;

    private String email;

    private Integer gender;

    private LocalDate birthday;

    private Integer status;
}
