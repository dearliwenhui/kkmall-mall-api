package com.ab.kkmallapimall.service;

import com.ab.kkmallapimall.common.Constants;
import com.ab.kkmallapimall.dto.request.AddressSaveRequest;
import com.ab.kkmallapimall.entity.Address;
import com.ab.kkmallapimall.exception.BusinessException;
import com.ab.kkmallapimall.exception.ErrorCode;
import com.ab.kkmallapimall.mapper.AddressMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 地址服务
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressMapper addressMapper;

    /**
     * 获取地址列表
     */
    public List<Address> getAddressList(Long userId) {
        return addressMapper.selectList(
                new LambdaQueryWrapper<Address>()
                        .eq(Address::getUserId, userId)
                        .orderByDesc(Address::getIsDefault)
                        .orderByDesc(Address::getCreateTime)
        );
    }

    /**
     * 获取地址详情
     */
    public Address getAddressDetail(Long userId, Long id) {
        Address address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
        }
        return address;
    }

    /**
     * 新增地址
     */
    @Transactional(rollbackFor = Exception.class)
    public Address createAddress(Long userId, AddressSaveRequest request) {
        Address address = new Address();
        BeanUtils.copyProperties(request, address);
        address.setUserId(userId);
        address.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()) ?
                Constants.IsDefault.YES : Constants.IsDefault.NO);

        // 如果设置为默认地址，先取消其他默认地址
        if (address.getIsDefault() == Constants.IsDefault.YES) {
            cancelOtherDefaultAddress(userId);
        }

        addressMapper.insert(address);
        return address;
    }

    /**
     * 更新地址
     */
    @Transactional(rollbackFor = Exception.class)
    public Address updateAddress(Long userId, Long id, AddressSaveRequest request) {
        Address address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
        }

        BeanUtils.copyProperties(request, address);
        address.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()) ?
                Constants.IsDefault.YES : Constants.IsDefault.NO);

        // 如果设置为默认地址，先取消其他默认地址
        if (address.getIsDefault() == Constants.IsDefault.YES) {
            cancelOtherDefaultAddress(userId, id);
        }

        int affected = addressMapper.updateById(address);
        ensureUpdated(affected, "数据已变化，请刷新后重试");
        return address;
    }

    /**
     * 删除地址
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAddress(Long userId, Long id) {
        Address address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
        }
        addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                .set(Address::getDeleted, Instant.now().toEpochMilli())
                .eq(Address::getId, id)
                .isNull(Address::getDeleted));
    }

    /**
     * 设置默认地址
     */
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultAddress(Long userId, Long id) {
        Address address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
        }

        // 取消其他默认地址
        cancelOtherDefaultAddress(userId, id);

        // 设置为默认地址
        address.setIsDefault(Constants.IsDefault.YES);
        int affected = addressMapper.updateById(address);
        ensureUpdated(affected, "数据已变化，请刷新后重试");
    }

    /**
     * 取消其他默认地址
     */
    private void cancelOtherDefaultAddress(Long userId) {
        cancelOtherDefaultAddress(userId, null);
    }

    /**
     * 取消其他默认地址（排除指定ID）
     */
    private void cancelOtherDefaultAddress(Long userId, Long excludeId) {
        LambdaUpdateWrapper<Address> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(Address::getIsDefault, Constants.IsDefault.NO)
                .eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, Constants.IsDefault.YES);

        if (excludeId != null) {
            wrapper.ne(Address::getId, excludeId);
        }

        addressMapper.update(null, wrapper);
    }

    private void ensureUpdated(int affected, String message) {
        if (affected > 0) {
            return;
        }
        throw new BusinessException(409, message);
    }
}
