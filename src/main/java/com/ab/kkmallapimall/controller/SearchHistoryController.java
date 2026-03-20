package com.ab.kkmallapimall.controller;

import com.ab.kkmallapimall.common.Result;
import com.ab.kkmallapimall.service.SearchHistoryService;
import com.ab.kkmallapimall.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 搜索历史控制器
 */
@Tag(name = "搜索历史", description = "搜索历史相关接口")
@RestController
@RequestMapping("/api/mall/search-history")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "添加搜索历史")
    @PostMapping
    public Result<Void> addSearchHistory(@RequestParam String keyword) {
        Long userId = securityUtils.getCurrentUserId();
        searchHistoryService.addSearchHistory(userId, keyword);
        return Result.success(null);
    }

    @Operation(summary = "获取搜索历史")
    @GetMapping
    public Result<List<String>> getSearchHistory(@RequestParam(defaultValue = "10") Integer limit) {
        Long userId = securityUtils.getCurrentUserId();
        List<String> history = searchHistoryService.getSearchHistory(userId, limit);
        return Result.success(history);
    }

    @Operation(summary = "清空搜索历史")
    @DeleteMapping
    public Result<Void> clearSearchHistory() {
        Long userId = securityUtils.getCurrentUserId();
        searchHistoryService.clearSearchHistory(userId);
        return Result.success("清空成功", null);
    }

    @Operation(summary = "删除单条搜索历史")
    @DeleteMapping("/{keyword}")
    public Result<Void> deleteSearchHistory(@PathVariable String keyword) {
        Long userId = securityUtils.getCurrentUserId();
        searchHistoryService.deleteSearchHistory(userId, keyword);
        return Result.success("删除成功", null);
    }
}
