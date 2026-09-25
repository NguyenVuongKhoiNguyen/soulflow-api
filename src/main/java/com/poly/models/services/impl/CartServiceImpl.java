package com.poly.models.services.impl;

import java.time.LocalDateTime;
import java.util.List;

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

import com.poly.models.entities.Cart;
import com.poly.models.enums.SortOrder;
import com.poly.models.mappers.CartMapper;
import com.poly.models.repositories.CartRepository;
import com.poly.models.requests.CartRequest;
import com.poly.models.responses.CartResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.services.CartService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepo;                    
    private final CartMapper cartMapper;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    @CachePut(value = "cartList", key = "#result.id")
    @CacheEvict(value = "cartPages", allEntries = true)
    public CartResponse save(CartRequest request) {
        Cart incoming = cartMapper.toEntity(request);

        if (request.getId() == null) {
            Cart saved = cartRepo.save(incoming);
            return cartMapper.toResponse(saved);
        }

        Cart existing = cartRepo.findById(request.getId())
            .orElseThrow(() -> new EntityNotFoundException("Cart not found with id: " + request.getId()));

        existing.getItems().clear();
        cartRepo.flush();

        incoming.getItems().forEach(item -> item.setCart(existing));
        existing.getItems().addAll(incoming.getItems());
        existing.calTotal();

        return cartMapper.toResponse(existing);
    }
    
    @Override
    @Transactional
    @Caching(evict = {
    	@CacheEvict(value = "cartList", key = "#cartId"),
    	@CacheEvict(value = "cartPages", allEntries = true)
    })
    public void softDeleteById(Long cartId) {
		// TODO Auto-generated method stub
        cartRepo.softDelete(cartId);
    }

    @Override
    @Cacheable(value = "cartList", key = "#cartId")
    public CartResponse findById(Long cartId) {
        // TODO Auto-generated method stub
        if (cartId == null) throw new IllegalArgumentException("Can't not find cart when id is null");
        Cart cart = cartRepo.findById(cartId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found with id: " + cartId));
        return cartMapper.toResponse(cart);
    }

	@Override
	@Cacheable(value = "cartPages", key = "#accountId + '_' + #keyword + '_' + #fromDate + '_' + #toDate + '_' + #expired + '_' + #deleted + '_' + #sortOrder + '_' + #pageNumber + '_' + #pageSize")
	public PageResponse<CartResponse> filterAndPaginateCarts(Long accountId, String keyword, LocalDateTime fromDate, LocalDateTime toDate, Boolean expired, Boolean deleted, SortOrder sortOrder, Integer pageNumber, Integer pageSize) {
		// TODO Auto-generated method stub
		Sort sort = sortOrder == SortOrder.ASC
	            ? Sort.by("id").ascending()
	            : Sort.by("id").descending();
		Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
		Page<Cart> page = cartRepo.filterCarts(accountId, keyword, fromDate, toDate, expired, deleted, pageable);
		List<CartResponse> responses = cartMapper.toResponseList(page.getContent());
		return new PageResponse<>(page, responses);
	}

    @Override
    public void checkAndExpireBeforePagination(String keyword, LocalDateTime fromDate, LocalDateTime toDate, Boolean expired, Boolean deleted) {
        int effectedRows = cartRepo.checkAndExpireBeforePagination(keyword, fromDate, toDate, expired, deleted);
        if (effectedRows != 0) {
            Cache cache = cacheManager.getCache("cartPages");
            if (cache != null) cache.clear();
            Cache detailCache = cacheManager.getCache("cartList");
            if (detailCache != null) detailCache.clear();
        }
    }
}
