package com.ab.kkmallapimall.service;

import com.ab.kkmallapimall.common.PageResult;
import com.ab.kkmallapimall.entity.PointsLog;
import com.ab.kkmallapimall.entity.MallUser;
import com.ab.kkmallapimall.mapper.PointsLogMapper;
import com.ab.kkmallapimall.mapper.MallUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 积分服务
 */
@Service
@RequiredArgsConstructor
public class PointsService {

    private final PointsLogMapper pointsLogMapper;
    private final MallUserMapper mallUserMapper;

    /**
     * 添加积分
     */
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Long userId, Integer points, Integer type, Long orderId, String description) {
        if (points <= 0) {
            return;
        }

        // 更新用户积分
        MallUser user = mallUserMapper.selectById(userId);
        if (user != null) {
            Integer currentPoints = user.getPoints() != null ? user.getPoints() : 0;
            user.setPoints(currentPoints + points);
            mallUserMapper.updateById(user);
        }

        // 记录积分日志
        PointsLog log = new PointsLog();
        log.setUserId(userId);
        log.setPoints(points);
        log.setType(type);
        log.setOrderId(orderId);
        log.setDescription(description);
        pointsLogMapper.insert(log);
    }

    /**
     * 扣减积分
     */
    @Transactional(rollbackFor = Exception.class)
    public void deductPoints(Long userId, Integer points, Integer type, Long orderId, String description) {
        if (points <= 0) {
            return;
        }

        // 更新用户积分
        MallUser user = mallUserMapper.selectById(userId);
        if (user != null) {
            Integer currentPoints = user.getPoints() != null ? user.getPoints() : 0;
            if (currentPoints < points) {
                throw new RuntimeException("积分不足");
            }
            user.setPoints(currentPoints - points);
            mallUserMapper.updateById(user);
        }

        // 记录积分日志（负数）
        PointsLog log = new PointsLog();
        log.setUserId(userId);
        log.setPoints(-points);
        log.setType(type);
        log.setOrderId(orderId);
        log.setDescription(description);
        pointsLogMapper.insert(log);
    }

    /**
     * 获取用户积分
     */
    public Integer getUserPoints(Long userId) {
        MallUser user = mallUserMapper.selectById(userId);
        return user != null && user.getPoints() != null ? user.getPoints() : 0;
    }

    /**
     * 获取积分记录
     */
    public PageResult<Map<String, Object>> getPointsLog(Long userId, Integer pageNum, Integer pageSize) {
        Page<PointsLog> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<PointsLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsLog::getUserId, userId)
                .orderByDesc(PointsLog::getCreateTime);

        Page<PointsLog> logPage = pointsLogMapper.selectPage(page, wrapper);

        List<Map<String, Object>> resultList = logPage.getRecords().stream()
                .map(log -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", log.getId());
                    map.put("points", log.getPoints());
                    map.put("type", log.getType());
                    map.put("typeName", getTypeName(log.getType()));
                    map.put("description", log.getDescription());
                    map.put("createTime", log.getCreateTime());
                    return map;
                })
                .collect(Collectors.toList());

        return PageResult.of(pageNum, pageSize, logPage.getTotal(), resultList);
    }

    /**
     * 签到获取积分
     */
    @Transactional(rollbackFor = Exception.class)
    public void signIn(Long userId) {
        // 检查今天是否已签到
        // 简化实现：每次签到给5积分
        addPoints(userId, 5, 1, null, "每日签到");
    }

    /**
     * 获取积分类型名称
     */
    private String getTypeName(Integer type) {
        return switch (type) {
            case 1 -> "签到";
            case 2 -> "购物";
            case 3 -> "评价";
            case 4 -> "兑换";
            default -> "其他";
        };
    }
}
