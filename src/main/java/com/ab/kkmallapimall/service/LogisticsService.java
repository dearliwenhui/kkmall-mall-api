package com.ab.kkmallapimall.service;

import com.ab.kkmallapimall.dto.LogisticsVO;
import com.ab.kkmallapimall.entity.Order;
import com.ab.kkmallapimall.exception.BusinessException;
import com.ab.kkmallapimall.exception.ErrorCode;
import com.ab.kkmallapimall.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 物流服务
 */
@Service
@RequiredArgsConstructor
public class LogisticsService {

    private final OrderMapper orderMapper;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 查询物流信息
     * 注意：这是简化版实现，返回模拟数据
     * 生产环境需要集成快递100或快递鸟API
     */
    public LogisticsVO getLogistics(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        if (!StringUtils.hasText(order.getLogisticsCompany()) ||
                !StringUtils.hasText(order.getTrackingNumber())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        LogisticsVO vo = new LogisticsVO();
        vo.setLogisticsCompany(order.getLogisticsCompany());
        vo.setTrackingNumber(order.getTrackingNumber());
        vo.setShipTime(order.getShipTime());

        // 模拟物流轨迹数据
        List<LogisticsVO.LogisticsTrace> traces = new ArrayList<>();

        if (order.getShipTime() != null) {
            LogisticsVO.LogisticsTrace trace1 = new LogisticsVO.LogisticsTrace();
            trace1.setTime(order.getShipTime().format(FORMATTER));
            trace1.setStatus("已发货");
            trace1.setDescription("您的订单已由" + order.getLogisticsCompany() + "揽收");
            traces.add(trace1);

            LogisticsVO.LogisticsTrace trace2 = new LogisticsVO.LogisticsTrace();
            trace2.setTime(order.getShipTime().plusHours(2).format(FORMATTER));
            trace2.setStatus("运输中");
            trace2.setDescription("快件已到达【北京转运中心】");
            traces.add(trace2);

            if (order.getStatus() >= 3) { // 已完成
                LogisticsVO.LogisticsTrace trace3 = new LogisticsVO.LogisticsTrace();
                trace3.setTime(order.getConfirmTime() != null ?
                        order.getConfirmTime().format(FORMATTER) :
                        order.getShipTime().plusDays(1).format(FORMATTER));
                trace3.setStatus("已签收");
                trace3.setDescription("快件已签收，感谢使用" + order.getLogisticsCompany());
                traces.add(trace3);
                vo.setStatus("已签收");
            } else {
                vo.setStatus("运输中");
            }
        }

        vo.setTraces(traces);
        return vo;
    }
}
