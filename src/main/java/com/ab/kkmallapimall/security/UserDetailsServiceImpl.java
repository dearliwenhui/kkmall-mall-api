package com.ab.kkmallapimall.security;

import com.ab.kkmallapimall.entity.MallUser;
import com.ab.kkmallapimall.mapper.MallUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 用户详情服务实现
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final MallUserMapper mallUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // @TableLogic 会自动添加 deleted IS NULL 条件
        MallUser mallUser = mallUserMapper.selectOne(
                new LambdaQueryWrapper<MallUser>()
                        .eq(MallUser::getUsername, username)
        );

        if (mallUser == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        return User.builder()
                .username(mallUser.getUsername())
                .password(mallUser.getPassword())
                .authorities(Collections.emptyList())
                .accountExpired(false)
                .accountLocked(mallUser.getStatus() == 0)
                .credentialsExpired(false)
                .disabled(mallUser.getStatus() == 0)
                .build();
    }
}
