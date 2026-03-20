package com.ab.kkmallapimall.service;

import com.ab.kkmallapimall.dto.response.CategoryTreeVO;
import com.ab.kkmallapimall.entity.Category;
import com.ab.kkmallapimall.exception.BusinessException;
import com.ab.kkmallapimall.exception.ErrorCode;
import com.ab.kkmallapimall.mapper.CategoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分类服务
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;

    /**
     * 获取分类树
     */
    public List<CategoryTreeVO> getCategoryTree() {
        // 查询所有分类
        List<Category> allCategories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSort)
                        .orderByAsc(Category::getId)
        );

        // 构建树形结构
        return buildTree(allCategories, 0L);
    }

    /**
     * 获取分类详情
     */
    public Category getCategoryDetail(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return category;
    }

    /**
     * 构建分类树
     */
    private List<CategoryTreeVO> buildTree(List<Category> allCategories, Long parentId) {
        return allCategories.stream()
                .filter(category -> parentId.equals(category.getParentId()))
                .map(category -> {
                    CategoryTreeVO vo = new CategoryTreeVO();
                    BeanUtils.copyProperties(category, vo);

                    // 递归查询子分类
                    List<CategoryTreeVO> children = buildTree(allCategories, category.getId());
                    vo.setChildren(children.isEmpty() ? null : children);

                    return vo;
                })
                .collect(Collectors.toList());
    }
}
