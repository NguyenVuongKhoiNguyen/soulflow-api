package com.poly.models.services.impl;

import com.poly.models.entities.ProductImage;
import com.poly.models.enums.SortOrder;
import com.poly.models.mappers.ProductImageMapper;
import com.poly.models.repositories.ProductImageRepository;
import com.poly.models.requests.ProductImageRequest;
import com.poly.models.responses.PageResponse;
import com.poly.models.responses.ProductImageResponse;
import com.poly.models.services.ProductImageService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductImageServiceImpl implements ProductImageService {

	private final ProductImageMapper productImageMapper;
	
	private final ProductImageRepository productImageRepo;

	@Override
	@Transactional
	@CachePut(value = "productImageList", key = "#result.id")
	@Caching(evict = {
		@CacheEvict(value = "productImagePages", allEntries = true),
		@CacheEvict(value = "productPages", allEntries = true),
		@CacheEvict(value = "productList", allEntries = true),
		@CacheEvict(value = "productDetailList", allEntries = true)
	})
	public ProductImageResponse save(ProductImageRequest request) {
		// TODO Auto-generated method stub
		ProductImage productImage = productImageMapper.toEntity(request);
		ProductImage saved = productImageRepo.save(productImage);
		return productImageMapper.toResponse(saved);
	}

	@Override
	@Cacheable(value = "productImageList", key = "#productImageId")
	public ProductImageResponse findById(Long productImageId) {
		if (productImageId == null) throw new IllegalArgumentException("Product Image ID is null");
		ProductImage exist = productImageRepo.findById(productImageId).orElseThrow(
			() -> new EntityNotFoundException("Product Image not found with Id: "+ productImageId)
		);
		return productImageMapper.toResponse(exist);
	}

	@Override
	@Transactional
	@Caching(evict = {
		@CacheEvict(value = "productImageList", key = "#id"),
		@CacheEvict(value = "productImagePages", allEntries = true),
		@CacheEvict(value = "productPages", allEntries = true),
		@CacheEvict(value = "productList", allEntries = true),
		@CacheEvict(value = "productDetailList", allEntries = true)
	})
	public void softDeleteById(Long id) {
		// TODO Auto-generated method stub
		productImageRepo.softDelete(id);
	}

	@Override
	@Cacheable(value = "productImagePages", key = "#keyword + '_' + #sortOrder + '_' + #pageNumber + '_' + #pageSize")
	public PageResponse<ProductImageResponse> filterAndPagonateProductImages(
		String keyword,
		SortOrder sortOrder,
		Integer pageNumber,
		Integer pageSize
	) {
		Sort sort = sortOrder == SortOrder.ASC ? Sort.by("id").ascending() : Sort.by("id").descending();
		Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
		Page<ProductImage> page = productImageRepo.filterProductImages(keyword, pageable);
		List<ProductImageResponse> responses = productImageMapper.toResponseList(page.getContent());
		return new PageResponse<>(page, responses);
	}
}
