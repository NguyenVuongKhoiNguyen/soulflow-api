package com.poly.models.services.impl;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.models.entities.Category;
import com.poly.models.enums.SortOrder;
import com.poly.models.repositories.CategoryRepository;
import com.poly.models.requests.CategoryRequest;
import com.poly.models.responses.CategoryResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.services.CategoryService;
import com.poly.models.mappers.CategoryMappper;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepo;
    
    private final CategoryMappper categoryMapper;
    
    @Override
    @Transactional
    @CachePut(value = "categoryList", key = "#result.id")
    @Caching(evict = {
        @CacheEvict(value = "categoryPages", allEntries = true),
        @CacheEvict(value = "categories", allEntries = true)
    })
    public CategoryResponse save(CategoryRequest request) {
        Category category = categoryMapper.toEntity(request);
        Category saved = categoryRepo.save(category);
        return categoryMapper.toBasicResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "categoryList", key = "#categoryId"),
        @CacheEvict(value = "categoryPages", allEntries = true),
        @CacheEvict(value = "categories", allEntries = true)
    })
    public void softDeleteById(Long categoryId) {
        categoryRepo.softDelete(categoryId);
    }

    @Override
    @Cacheable(value = "categoryList", key = "#categoryId")
    public CategoryResponse findById(Long categoryId) {
        if (categoryId == null) throw new IllegalArgumentException("Can't not find category when id is null");
        Category exist = categoryRepo.findById(Long.valueOf(categoryId))
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + categoryId));
        return categoryMapper.toBasicResponse(exist);
    }

    @Override
    @Cacheable(value = "categories")
    public List<CategoryResponse> findAll() {
        List<Category> categories = categoryRepo.findAll();
        return categoryMapper.toBasicResponseList(categories);
    }

    @Override
    @Cacheable(value = "categoryPages", key = "#keyword + '_' + #deleted + '_' + #sortOrder + '_' + #pageNumber + '_' + #pageSize")
    public PageResponse<CategoryResponse> filterAndPaginateCategories(String keyword, Boolean deleted, SortOrder sortOrder, Integer pageNumber, Integer pageSize) {
    	Sort sort = sortOrder == SortOrder.ASC
	            ? Sort.by("id").ascending()
	            : Sort.by("id").descending();
    	Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
    	Page<Category> page = categoryRepo.filterCategories(keyword, deleted, pageable);
    	List<CategoryResponse> responses = categoryMapper.toBasicResponseList(page.getContent());
        return new PageResponse<>(page, responses);
    }

}