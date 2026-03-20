package com.ab.kkmallapimall.util;

import com.ab.kkmallapimall.entity.MallUser;
import com.ab.kkmallapimall.exception.BusinessException;
import com.ab.kkmallapimall.exception.ErrorCode;
import com.ab.kkmallapimall.mapper.MallUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * 安全工具类
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final MallUserMapper mallUserMapper;

    /**
     * 获取当前登录用户
     */
    public MallUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String username = ((UserDetails) principal).getUsername();
        MallUser user = mallUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MallUser>()
                        .eq(MallUser::getUsername, username)
        );

        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return user;
    }

    /**
     * 获取当前登录用户ID
     */
    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
