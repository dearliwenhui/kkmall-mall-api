package com.ab.kkmallapimall.mapper;

import com.ab.kkmallapimall.entity.Payment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付记录 Mapper 接口
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}
