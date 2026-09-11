package com.poly.models.services;

import java.util.List;

import com.poly.models.enums.SortOrder;
import com.poly.models.requests.CategoryRequest;
import com.poly.models.responses.CategoryResponse;
import com.poly.models.responses.PageResponse;

public interface CategoryService {
	CategoryResponse save(CategoryRequest request);
	void softDeleteById(Long categoryId);
	CategoryResponse findById(Long categoryId);
	List<CategoryResponse> findAll();
	PageResponse<CategoryResponse> filterAndPaginateCategories(
            String keyword,
            Boolean deleted,
            SortOrder sortOrder,
            Integer pageNumber,
            Integer pageSize
    );
}
