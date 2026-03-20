package com.ab.kkmallapimall.controller;

import com.ab.kkmallapimall.common.Result;
import com.ab.kkmallapimall.dto.request.AddressSaveRequest;
import com.ab.kkmallapimall.entity.Address;
import com.ab.kkmallapimall.service.AddressService;
import com.ab.kkmallapimall.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收货地址控制器
 */
@Tag(name = "收货地址管理", description = "收货地址相关接口")
@RestController
@RequestMapping("/api/mall/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "获取地址列表")
    @GetMapping
    public Result<List<Address>> getAddressList() {
        Long userId = securityUtils.getCurrentUserId();
        List<Address> addresses = addressService.getAddressList(userId);
        return Result.success(addresses);
    }

    @Operation(summary = "获取地址详情")
    @GetMapping("/{id}")
    public Result<Address> getAddressDetail(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        Address address = addressService.getAddressDetail(userId, id);
        return Result.success(address);
    }

    @Operation(summary = "新增地址")
    @PostMapping
    public Result<Address> createAddress(@Valid @RequestBody AddressSaveRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        Address address = addressService.createAddress(userId, request);
        return Result.success(address);
    }

    @Operation(summary = "更新地址")
    @PutMapping("/{id}")
    public Result<Address> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressSaveRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        Address address = addressService.updateAddress(userId, id, request);
        return Result.success(address);
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/{id}")
    public Result<Void> deleteAddress(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        addressService.deleteAddress(userId, id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "设置默认地址")
    @PutMapping("/{id}/default")
    public Result<Void> setDefaultAddress(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        addressService.setDefaultAddress(userId, id);
        return Result.success("设置成功", null);
    }
}
