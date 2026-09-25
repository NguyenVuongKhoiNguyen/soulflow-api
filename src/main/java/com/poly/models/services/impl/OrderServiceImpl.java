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
import com.poly.models.repositories.OrderDetailRepository;
import com.poly.models.repositories.StoreRepository;
import com.poly.models.requests.OrderRequest;
import com.poly.models.requests.AdminOrderUpdateRequest;
import com.poly.models.responses.OrderResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.services.OrderService;
import com.poly.models.services.ShippingFeeService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

	private final OrderMapper orderMapper;
	
    private final OrderRepository orderRepo;

    private final OrderDetailRepository orderDetailRepository;

    private final StoreRepository storeRepository;

    private final ShippingFeeService shippingFeeService;

    private final CacheManager cacheManager;

    @Override
    @Transactional
    @CachePut(value = "orderList", key = "#result.id")
    @Caching(evict = {
        @CacheEvict(value = "orderPages", allEntries = true),
		@CacheEvict(value = "productPages", allEntries = true),
		@CacheEvict(value = "productList", allEntries = true),
		@CacheEvict(value = "productDetailList", allEntries = true)
    })
    public OrderResponse save(OrderRequest resquest) {
        Order order = orderMapper.toEntity(resquest);
        if (resquest.getId() == null && order.getStore() != null) {
            order.setShippingFee(shippingFeeService.calculateFee(
                order.getStore().getAddress(),
                order.getAddress()
            ));
            order.calTotal();
            if (order.getPayment() != null) {
                order.getPayment().setAmount(order.getTotal());
            }
        }
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
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "orderList", key = "#orderId"),
        @CacheEvict(value = "orderPages", allEntries = true)
    })
    public OrderResponse updateAdminInfo(Long orderId, AdminOrderUpdateRequest request) {
        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found with Id: " + orderId));

        if (order.getStatus() == OrderStatus.CANCELLED && request.getStatus() != OrderStatus.CANCELLED) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT,
                "A cancelled order cannot be reopened"
            );
        }

        if (order.getStatus() != OrderStatus.CANCELLED && request.getStatus() == OrderStatus.CANCELLED) {
            orderDetailRepository.restoreProductQuantity(orderId);
        }

        order.setFullname(request.getFullname());
        order.setPhone(request.getPhone());
        order.setAddress(request.getAddress());
        order.setShippingFee(request.getShippingFee() == null ? java.math.BigDecimal.ZERO : request.getShippingFee());
        if (request.getStoreId() != null) {
            order.setStore(storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new EntityNotFoundException("Store not found with id: " + request.getStoreId())));
        }
        order.setStatus(request.getStatus());
        order.calTotal();
        if (order.getPayment() != null) {
            order.getPayment().setAmount(order.getTotal());
        }
        return orderMapper.toResponse(orderRepo.save(order));
    }

	@Override
	public PageResponse<OrderResponse> findMine(String username, Integer pageNumber, Integer pageSize) {
		Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("id").descending());
		Page<Order> page = orderRepo.findActiveByUsername(username, pageable);
		return new PageResponse<>(page, orderMapper.toResponseList(page.getContent()));
	}

    @Override
    public OrderResponse findMineById(String username, Long orderId) {
        Order order = orderRepo.findActiveByIdAndUsername(orderId, username)
            .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        return orderMapper.toResponse(order);
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
			if (cache != null) cache.clear();
			Cache detailCache = cacheManager.getCache("orderList");
			if (detailCache != null) detailCache.clear();
        }
    }

    @Override
	@Transactional
	@Caching(evict = {
		@CacheEvict(value = "orderList", key = "#orderId"),
		@CacheEvict(value = "orderPages", allEntries = true)
	})
    public Integer markOrderAsPaidIfFullyPaid(Long orderId) {
        
        return orderRepo.markOrderAsPaidIfFullyPaid(orderId);
    }
}


