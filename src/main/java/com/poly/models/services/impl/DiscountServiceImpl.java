package com.poly.models.services.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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

import com.poly.models.entities.Discount;
import com.poly.models.entities.Product;
import com.poly.models.enums.SortOrder;
import com.poly.models.mappers.DiscountMapper;
import com.poly.models.mappers.ProductMapper;
import com.poly.models.repositories.ProductRepository;
import com.poly.models.repositories.DiscountRepository;
import com.poly.models.requests.DiscountRequest;
import com.poly.models.responses.DiscountResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.responses.ProductResponse;
import com.poly.models.services.DiscountService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiscountServiceImpl implements DiscountService {

    private final DiscountRepository discountRepo;

    private final DiscountMapper discountMapper;

    private final ProductRepository productRepo;

    private final ProductMapper productMapper;

    private final CacheManager cacheManager;

    @Override
    @Transactional
    @CachePut(value = "discountList", key = "#result.id")
    @CacheEvict(value = "discountPages", allEntries = true)
    public DiscountResponse save(DiscountRequest request) {
        Discount discount = discountMapper.toEntity(request);
        discount.setExpired(discount.getExpiredDate() != null
            && discount.getExpiredDate().isBefore(LocalDateTime.now()));
        Discount saved = discountRepo.save(discount);
        return discountMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "discountList", key = "#discountId"),
        @CacheEvict(value = "discountPages", allEntries = true)
    })
    public void softDeleteById(Long discountId) {
        discountRepo.softDelete(discountId);
    }

    @Override
    @Cacheable(value = "discountList", key = "#discountId")
    public DiscountResponse findById(Long discountId) {
        if (discountId == null) throw new IllegalArgumentException("Can't not find discount when id is null");
        Discount discount = discountRepo.findById(discountId)
                .orElseThrow(() -> new EntityNotFoundException("Discount not found with id: " + discountId));
        DiscountResponse response = discountMapper.toResponse(discount);
        response.setProductResponses(productMapper.toBasicResponseList(discount.getProducts()));
        return response;
    }

    @Override
    @Cacheable(value = "discountPages", key = "#keyword + '_' + #fromDate + '_' + #toDate + '_' + #expired + '_' + #deleted + '_' + #sortOrder + '_' + #pageNumber + '_' + #pageSize")
    public PageResponse<DiscountResponse> filterAndPaginateDiscounts(String keyword, LocalDateTime fromDate, LocalDateTime toDate,
            Boolean expired, Boolean deleted, SortOrder sortOrder, Integer pageNumber, Integer pageSize) {
        Sort sort = sortOrder == SortOrder.ASC
                ? Sort.by("id").ascending()
                : Sort.by("id").descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        Page<Discount> page = discountRepo.filterDiscounts(keyword, fromDate, toDate, expired, deleted, pageable);
        
        List<List<Product>> productLists = new ArrayList<>();
        for (int i = 0; i < page.getContent().size(); i++) {
            if (page.getContent().get(i) != null) { 
                productLists.add(page.getContent().get(i).getProducts());
            }
        }

        List<List<ProductResponse>> productResponseLists = new ArrayList<>();
        for (int i = 0; i < productLists.size(); i++) {
            if (productLists.get(i) != null) {
                productResponseLists.add(productMapper.toBasicResponseList(productLists.get(i)));
            }
        }

        List<DiscountResponse> responses = discountMapper.toResponseList(page.getContent());
        for (int i = 0; i < productResponseLists.size(); i++) {
            if (productResponseLists.get(i) != null) {
                responses.get(i).setProductResponses(productResponseLists.get(i));
            }
        }

        return new PageResponse<>(page, responses);
    }

    @Override
    public void checkAndExpireBeforePagination(String keyword, LocalDateTime fromDate, LocalDateTime toDate, Boolean expired, Boolean deleted) {
        int effectedRows = discountRepo.checkAndExpireBeforePagination(keyword, fromDate, fromDate, expired, deleted);
        if (effectedRows != 0) {
            Cache cache = cacheManager.getCache("discountPages");
            cache.clear();
        }
    }

    @Transactional
    @Override
    @Caching(
        evict = {
            @CacheEvict(value = "productList", key = "#productId"),
            @CacheEvict(value = "productPages", allEntries = true),
            @CacheEvict(value = "discountList", allEntries = true),
            @CacheEvict(value = "discountPages", allEntries = true)
        }
    )
    public ProductResponse addDiscounts(Long productId, List<Long> discountIds) {

        Product product = productRepo.findById(productId).orElseThrow(
            () -> new EntityNotFoundException("Product not found with id: " + productId)
        );

        Set<Long> uniqueDiscountIds = new LinkedHashSet<>(discountIds);
        List<Discount> discounts = new ArrayList<>();
        for (Long id : uniqueDiscountIds) {
            Discount discount = discountRepo.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Discount not found with Id:" + id)
            );
            discounts.add(discount);
        }

        product.setDiscounts(discounts);
        Product saved = productRepo.save(product);
        return productMapper.toDetailedResponse(saved);
    }

    @Transactional
    @Override
    @Caching(evict = {
        @CacheEvict(value = "productList", allEntries = true),
        @CacheEvict(value = "productPages", allEntries = true),
        @CacheEvict(value = "discountList", allEntries = true),
        @CacheEvict(value = "discountPages", allEntries = true)
    })
    public DiscountResponse updateProducts(Long discountId, List<Long> productIds) {
        Discount discount = discountRepo.findById(discountId).orElseThrow(
            () -> new EntityNotFoundException("Discount not found with id: " + discountId)
        );

        Set<Long> requestedIds = new LinkedHashSet<>(productIds == null ? List.of() : productIds);
        List<Product> requestedProducts = productRepo.findAllById(requestedIds);
        if (requestedProducts.size() != requestedIds.size()) {
            Set<Long> foundIds = new LinkedHashSet<>();
            requestedProducts.forEach(product -> foundIds.add(product.getId()));
            requestedIds.removeAll(foundIds);
            throw new EntityNotFoundException("Products not found with ids: " + requestedIds);
        }

        List<Product> affectedProducts = new ArrayList<>(discount.getProducts());
        for (Product product : requestedProducts) {
            if (!affectedProducts.contains(product)) affectedProducts.add(product);
        }

        for (Product product : affectedProducts) {
            boolean shouldContainDiscount = requestedIds.contains(product.getId());
            if (shouldContainDiscount) {
                if (!product.getDiscounts().contains(discount)) product.getDiscounts().add(discount);
            } else {
                product.getDiscounts().remove(discount);
            }
        }
        productRepo.saveAll(affectedProducts);

        DiscountResponse response = discountMapper.toResponse(discount);
        response.setProductResponses(productMapper.toBasicResponseList(requestedProducts));
        return response;
    }
}
