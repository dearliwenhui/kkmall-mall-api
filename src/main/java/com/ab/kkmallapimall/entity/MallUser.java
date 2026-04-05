package com.ab.kkmallapimall.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商城用户实体
 */
@Data
@TableName("mall_user")
public class MallUser implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String avatar;

    private String phone;

    private String email;

    private Integer gender;

    private LocalDate birthday;

    private Integer status;

    private Integer points;

    @Version
    private Long version;

    /**
     * Logical delete flag: NULL for active records, timestamp (milliseconds) for deleted records.
     */
    @TableLogic(value = "NULL", delval = "UNIX_TIMESTAMP(NOW()) * 1000")
    private Long deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
