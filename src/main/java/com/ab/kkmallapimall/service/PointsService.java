package com.ab.kkmallapimall.service;

import com.ab.kkmallapimall.common.PageResult;
import com.ab.kkmallapimall.entity.MallUser;
import com.ab.kkmallapimall.entity.PointsLog;
import com.ab.kkmallapimall.exception.BusinessException;
import com.ab.kkmallapimall.mapper.MallUserMapper;
import com.ab.kkmallapimall.mapper.PointsLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 积分服务。
 */
@Service
@RequiredArgsConstructor
public class PointsService {

    private final PointsLogMapper pointsLogMapper;
    private final MallUserMapper mallUserMapper;

    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Long userId, Integer points, Integer type, Long orderId, String description) {
        if (points <= 0) {
            return;
        }

        MallUser user = mallUserMapper.selectById(userId);
        if (user != null) {
            Integer currentPoints = user.getPoints() != null ? user.getPoints() : 0;
            user.setPoints(currentPoints + points);
            int affected = mallUserMapper.updateById(user);
            ensureUpdated(affected, "数据已变化，请刷新后重试");
        }

        PointsLog log = new PointsLog();
        log.setUserId(userId);
        log.setPoints(points);
        log.setType(type);
        log.setOrderId(orderId);
        log.setDescription(description);
        pointsLogMapper.insert(log);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deductPoints(Long userId, Integer points, Integer type, Long orderId, String description) {
        if (points <= 0) {
            return;
        }

        MallUser user = mallUserMapper.selectById(userId);
        if (user != null) {
            Integer currentPoints = user.getPoints() != null ? user.getPoints() : 0;
            if (currentPoints < points) {
                throw new RuntimeException("积分不足");
            }
            user.setPoints(currentPoints - points);
            int affected = mallUserMapper.updateById(user);
            ensureUpdated(affected, "数据已变化，请刷新后重试");
        }

        PointsLog log = new PointsLog();
        log.setUserId(userId);
        log.setPoints(-points);
        log.setType(type);
        log.setOrderId(orderId);
        log.setDescription(description);
        pointsLogMapper.insert(log);
    }

    /**
     * 超时或取消订单后返还积分。
     */
    @Transactional(rollbackFor = Exception.class)
    public void restorePointsForOrder(Long userId, Long orderId, String description) {
        if (orderId == null) {
            return;
        }
        List<PointsLog> deductionLogs = pointsLogMapper.selectList(
                new LambdaQueryWrapper<PointsLog>()
                        .eq(PointsLog::getUserId, userId)
                        .eq(PointsLog::getOrderId, orderId)
                        .lt(PointsLog::getPoints, 0)
        );
        int pointsToRestore = deductionLogs.stream()
                .map(PointsLog::getPoints)
                .filter(points -> points != null && points < 0)
                .mapToInt(points -> -points)
                .sum();
        if (pointsToRestore > 0) {
            addPoints(userId, pointsToRestore, 5, orderId, description);
        }
    }

    public Integer getUserPoints(Long userId) {
        MallUser user = mallUserMapper.selectById(userId);
        return user != null && user.getPoints() != null ? user.getPoints() : 0;
    }

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

    @Transactional(rollbackFor = Exception.class)
    public void signIn(Long userId) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        Long count = pointsLogMapper.selectCount(
                new LambdaQueryWrapper<PointsLog>()
                        .eq(PointsLog::getUserId, userId)
                        .eq(PointsLog::getType, 1)
                        .ge(PointsLog::getCreateTime, startOfDay)
        );
        if (count != null && count > 0) {
            throw new RuntimeException("今日已签到");
        }

        addPoints(userId, 5, 1, null, "每日签到");
    }

    private String getTypeName(Integer type) {
        return switch (type) {
            case 1 -> "签到";
            case 2 -> "购物";
            case 3 -> "评价";
            case 4 -> "兑换";
            case 5 -> "订单返还";
            default -> "其他";
        };
    }

    private void ensureUpdated(int affected, String message) {
        if (affected > 0) {
            return;
        }
        throw new BusinessException(409, message);
    }
}
