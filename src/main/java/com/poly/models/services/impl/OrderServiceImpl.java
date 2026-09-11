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

import com.poly.models.entities.Order;
import com.poly.models.enums.OrderStatus;
import com.poly.models.enums.SortOrder;
import com.poly.models.mappers.OrderMapper;
import com.poly.models.repositories.OrderRepository;
import com.poly.models.requests.OrderRequest;
import com.poly.models.responses.OrderResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.services.OrderService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

	private final OrderMapper orderMapper;
	
    private final OrderRepository orderRepo;

    private final CacheManager cacheManager;

    @Override
    @Transactional
    @CachePut(value = "orderList", key = "#result.id")
    @Caching(evict = {
        @CacheEvict(value = "orderPages", allEntries = true),
        @CacheEvict(value = "productPages", allEntries = true)
    })
    public OrderResponse save(OrderRequest resquest) {
        Order order = orderMapper.toEntity(resquest);
        order.getOrderDetails().forEach(od -> System.out.println(od.getProduct()));
        Order saved = orderRepo.save(order);
        return orderMapper.toResponse(saved);
    }
    
    @Override
    @Transactional
    @Caching(evict = {
    	@CacheEvict(value = "orderList", key = "#orderId"),
    	@CacheEvict(value = "orderPages", allEntries = true),
        @CacheEvict(value = "productPages", allEntries = true)
    })
    public void softDeleteById(Long orderId) {
        orderRepo.softDelete(orderId);
    }

    @Override
    @Cacheable(value = "orderList", key = "#orderId")
    public OrderResponse findById(Long orderId) {
        if (orderId == null) throw new IllegalArgumentException("Can't not find order when id is null");
        Order exist = orderRepo.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with Id: " + orderId));
        return orderMapper.toResponse(exist);
    }

    @Override
    @Cacheable(value = "orderPages", key = "#keyword + '_' + #fromDate + '_' + #toDate + '_' + #status + '_' + #expired + '_' + #deleted + '_' + #sortOrder + '_' + #pageNumber + '_' + #pageSize")
    public PageResponse<OrderResponse> filterAndPaginateOrders(
            String keyword,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            OrderStatus status,
            Boolean expired,
            Boolean deleted,
            SortOrder sortOrder,
            Integer pageNumber,
            Integer pageSize) {
    	Sort sort = sortOrder == SortOrder.ASC
	            ? Sort.by("id").ascending()
	            : Sort.by("id").descending();
    	Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
    	Page<Order> page = orderRepo.filterOrders(keyword, fromDate, toDate, status, expired, deleted, pageable);
    	List<OrderResponse> responses = orderMapper.toResponseList(page.getContent());
        return new PageResponse<>(page, responses);
    }

    @Override
    public void checkAndExpireBeforePagination(
            String keyword,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            OrderStatus status,
            Boolean expired,
            Boolean deleted
    ) {
        int effectedRows = orderRepo.checkAndExpireBeforePagination(keyword, fromDate, toDate, expired, deleted);
        if (effectedRows != 0) {
            Cache cache = cacheManager.getCache("orderPages");
            cache.clear();
        }
    }

    @Override
    public Integer markOrderAsPaidIfFullyPaid(Long orderId) {
        
        return orderRepo.markOrderAsPaidIfFullyPaid(orderId);
    }
}
