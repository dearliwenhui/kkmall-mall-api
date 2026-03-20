package com.ab.kkmallapimall.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 分类树VO
 */
@Data
public class CategoryTreeVO {

    private Long id;

    private String name;

    private Long parentId;

    private Integer level;

    private Integer sort;

    private String icon;

    /**
     * 子分类列表
     */
    private List<CategoryTreeVO> children;
}
