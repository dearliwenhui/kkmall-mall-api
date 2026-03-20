package com.ab.kkmallapimall.service;

import com.ab.kkmallapimall.entity.SearchHistory;
import com.ab.kkmallapimall.mapper.SearchHistoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索历史服务
 */
@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private final SearchHistoryMapper searchHistoryMapper;

    /**
     * 添加搜索历史
     */
    @Transactional(rollbackFor = Exception.class)
    public void addSearchHistory(Long userId, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }

        // 检查是否已存在相同关键词
        SearchHistory existing = searchHistoryMapper.selectOne(
                new LambdaQueryWrapper<SearchHistory>()
                        .eq(SearchHistory::getUserId, userId)
                        .eq(SearchHistory::getKeyword, keyword)
                        .last("LIMIT 1")
        );

        if (existing != null) {
            // 如果已存在，删除旧记录
            searchHistoryMapper.deleteById(existing.getId());
        }

        // 添加新记录
        SearchHistory history = new SearchHistory();
        history.setUserId(userId);
        history.setKeyword(keyword);
        searchHistoryMapper.insert(history);

        // 保持最多20条记录
        List<SearchHistory> allHistory = searchHistoryMapper.selectList(
                new LambdaQueryWrapper<SearchHistory>()
                        .eq(SearchHistory::getUserId, userId)
                        .orderByDesc(SearchHistory::getCreateTime)
        );

        if (allHistory.size() > 20) {
            List<Long> idsToDelete = allHistory.stream()
                    .skip(20)
                    .map(SearchHistory::getId)
                    .collect(Collectors.toList());
            searchHistoryMapper.deleteBatchIds(idsToDelete);
        }
    }

    /**
     * 获取搜索历史
     */
    public List<String> getSearchHistory(Long userId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        List<SearchHistory> historyList = searchHistoryMapper.selectList(
                new LambdaQueryWrapper<SearchHistory>()
                        .eq(SearchHistory::getUserId, userId)
                        .orderByDesc(SearchHistory::getCreateTime)
                        .last("LIMIT " + limit)
        );

        return historyList.stream()
                .map(SearchHistory::getKeyword)
                .collect(Collectors.toList());
    }

    /**
     * 清空搜索历史
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearSearchHistory(Long userId) {
        searchHistoryMapper.delete(
                new LambdaQueryWrapper<SearchHistory>()
                        .eq(SearchHistory::getUserId, userId)
        );
    }

    /**
     * 删除单条搜索历史
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSearchHistory(Long userId, String keyword) {
        searchHistoryMapper.delete(
                new LambdaQueryWrapper<SearchHistory>()
                        .eq(SearchHistory::getUserId, userId)
                        .eq(SearchHistory::getKeyword, keyword)
        );
    }
}
