package com.ab.kkmallapimall.service;

import com.ab.kkmallapimall.common.Constants;
import com.ab.kkmallapimall.dto.request.LoginRequest;
import com.ab.kkmallapimall.dto.request.RegisterRequest;
import com.ab.kkmallapimall.dto.response.LoginResponse;
import com.ab.kkmallapimall.dto.response.UserInfoVO;
import com.ab.kkmallapimall.entity.MallUser;
import com.ab.kkmallapimall.exception.BusinessException;
import com.ab.kkmallapimall.exception.ErrorCode;
import com.ab.kkmallapimall.mapper.MallUserMapper;
import com.ab.kkmallapimall.security.JwtTokenProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MallUserMapper mallUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 用户注册
     */
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        // 检查用户名是否已存在（@TableLogic 会自动添加 deleted IS NULL 条件）
        Long usernameCount = mallUserMapper.selectCount(
                new LambdaQueryWrapper<MallUser>()
                        .eq(MallUser::getUsername, request.getUsername())
        );
        if (usernameCount > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // 检查手机号是否已存在（只在手机号不为空时检查）
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            Long phoneCount = mallUserMapper.selectCount(
                    new LambdaQueryWrapper<MallUser>()
                            .eq(MallUser::getPhone, request.getPhone())
            );
            if (phoneCount > 0) {
                throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
        }

        // 创建用户
        MallUser user = new MallUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setStatus(Constants.UserStatus.ENABLED);
        user.setGender(Constants.Gender.UNKNOWN);

        mallUserMapper.insert(user);
    }

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        // 查询用户（@TableLogic 会自动添加 deleted IS NULL 条件）
        MallUser user = mallUserMapper.selectOne(
                new LambdaQueryWrapper<MallUser>()
                        .eq(MallUser::getUsername, request.getUsername())
        );

        if (user == null) {
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 检查用户状态
        if (user.getStatus() == Constants.UserStatus.DISABLED) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        // 生成Token
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());

        // 构造用户信息
        UserInfoVO userInfo = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfo);

        return new LoginResponse(token, userInfo);
    }

    /**
     * 获取用户信息
     */
    public UserInfoVO getUserInfo(Long userId) {
        MallUser user = mallUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        UserInfoVO userInfo = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfo);
        return userInfo;
    }
}
