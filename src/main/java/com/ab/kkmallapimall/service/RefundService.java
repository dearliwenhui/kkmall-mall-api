package com.ab.kkmallapimall.service;

import com.ab.kkmallapimall.common.Constants;
import com.ab.kkmallapimall.common.PageResult;
import com.ab.kkmallapimall.dto.CreateRefundRequest;
import com.ab.kkmallapimall.dto.RefundVO;
import com.ab.kkmallapimall.entity.Order;
import com.ab.kkmallapimall.entity.OrderItem;
import com.ab.kkmallapimall.entity.Product;
import com.ab.kkmallapimall.entity.Refund;
import com.ab.kkmallapimall.exception.BusinessException;
import com.ab.kkmallapimall.exception.ErrorCode;
import com.ab.kkmallapimall.mapper.OrderItemMapper;
import com.ab.kkmallapimall.mapper.OrderMapper;
import com.ab.kkmallapimall.mapper.ProductMapper;
import com.ab.kkmallapimall.mapper.RefundMapper;
import com.ab.kkmallapimall.util.RefundNoGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 退款服务
 */
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundMapper refundMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;

    /**
     * 创建退款申请
     */
    @Transactional(rollbackFor = Exception.class)
    public RefundVO createRefund(Long userId, CreateRefundRequest request) {
        // 查询订单
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 验证订单状态（已支付的订单才能退款）
        if (order.getStatus() < Constants.OrderStatus.PENDING_SHIPMENT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        // 检查是否已有退款申请
        Refund existingRefund = refundMapper.selectOne(
                new LambdaQueryWrapper<Refund>()
                        .eq(Refund::getOrderId, request.getOrderId())
                        .in(Refund::getStatus, 0, 1) // 待审核或审核通过
        );
        if (existingRefund != null) {
            throw new BusinessException(ErrorCode.REFUND_STATUS_ERROR);
        }

        // 创建退款申请
        Refund refund = new Refund();
        refund.setRefundNo(RefundNoGenerator.generate());
        refund.setOrderId(order.getId());
        refund.setOrderNo(order.getOrderNo());
        refund.setUserId(userId);
        refund.setRefundAmount(request.getRefundAmount());
        refund.setRefundType(request.getRefundType());
        refund.setReason(request.getReason());
        refund.setDescription(request.getDescription());
        refund.setImages(request.getImages());
        refund.setStatus(0); // 待审核
        refundMapper.insert(refund);

        return convertToVO(refund);
    }

    /**
     * 获取退款列表
     */
    public PageResult<RefundVO> getRefundList(Long userId, Integer pageNum, Integer pageSize) {
        Page<Refund> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Refund> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Refund::getUserId, userId)
                .orderByDesc(Refund::getCreateTime);

        Page<Refund> refundPage = refundMapper.selectPage(page, wrapper);

        List<RefundVO> voList = refundPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(pageNum, pageSize, refundPage.getTotal(), voList);
    }

    /**
     * 获取退款详情
     */
    public RefundVO getRefundDetail(Long userId, Long refundId) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null || !refund.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.REFUND_NOT_FOUND);
        }
        return convertToVO(refund);
    }

    /**
     * 审核退款（管理后台）
     */
    @Transactional(rollbackFor = Exception.class)
    public void auditRefund(Long refundId, Integer status, String rejectReason) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException(ErrorCode.REFUND_NOT_FOUND);
        }

        if (refund.getStatus() != 0) {
            throw new BusinessException(ErrorCode.REFUND_STATUS_ERROR);
        }

        refund.setStatus(status);
        if (status == 2) { // 拒绝
            refund.setRejectReason(rejectReason);
        } else if (status == 1) { // 通过
            // 执行退款
            processRefund(refund);
        }
        refundMapper.updateById(refund);
    }

    /**
     * 处理退款
     */
    private void processRefund(Refund refund) {
        // 更新退款状态为退款成功
        refund.setStatus(3);
        refund.setRefundTime(LocalDateTime.now());

        // 恢复库存
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, refund.getOrderId())
        );

        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
                productMapper.updateById(product);
            }
        }

        // 更新订单状态
        Order order = orderMapper.selectById(refund.getOrderId());
        if (order != null) {
            order.setStatus(Constants.OrderStatus.CANCELLED);
            orderMapper.updateById(order);
        }
    }

    /**
     * 转换为VO
     */
    private RefundVO convertToVO(Refund refund) {
        RefundVO vo = new RefundVO();
        BeanUtils.copyProperties(refund, vo);
        vo.setRefundTypeName(refund.getRefundType() == 1 ? "仅退款" : "退货退款");
        vo.setStatusName(switch (refund.getStatus()) {
            case 0 -> "待审核";
            case 1 -> "审核通过";
            case 2 -> "审核拒绝";
            case 3 -> "退款成功";
            default -> "未知";
        });
        return vo;
    }
}
