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

import com.poly.models.entities.Store;
import com.poly.models.mappers.StoreMapper;
import com.poly.models.repositories.StoreRepository;
import com.poly.models.requests.StoreRequest;
import com.poly.models.responses.PageResponse;
import com.poly.models.responses.StoreResponse;
import com.poly.models.services.StoreService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final StoreMapper storeMapper;

    @Override
    @Transactional
    @CachePut(value = "storeList", key = "#result.id")
    @Caching(evict = {
        @CacheEvict(value = "stores", allEntries = true),
        @CacheEvict(value = "storePages", allEntries = true)
    })
    public StoreResponse save(StoreRequest request) {
        Store store = storeMapper.toEntity(request);
        if (store.getId() != null && !storeRepository.existsById(store.getId())) {
            throw new EntityNotFoundException("Store not found with id: " + store.getId());
        }
        return storeMapper.toResponse(storeRepository.save(store));
    }

    @Override
    @Cacheable(value = "storeList", key = "#storeId")
    public StoreResponse findById(Long storeId) {
        if (storeId == null) {
            throw new IllegalArgumentException("Store id is required");
        }
        return storeMapper.toResponse(storeRepository.findById(storeId)
            .orElseThrow(() -> new EntityNotFoundException("Store not found with id: " + storeId)));
    }

    @Override
    @Cacheable(value = "stores")
    public List<StoreResponse> findAll() {
        return storeMapper.toResponseList(storeRepository.findAll());
    }

    @Override
    @Cacheable(value = "storePages", key = "#id + '_' + #storeName + '_' + #pageNumber + '_' + #pageSize")
    public PageResponse<StoreResponse> filterAndPaginateStores(Long id, String storeName, Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("id").ascending());
        Page<Store> page = storeRepository.filterStores(id, storeName, pageable);
        return new PageResponse<>(page, storeMapper.toResponseList(page.getContent()));
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "storeList", key = "#storeId"),
        @CacheEvict(value = "stores", allEntries = true),
        @CacheEvict(value = "storePages", allEntries = true)
    })
    public void deleteById(Long storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new EntityNotFoundException("Store not found with id: " + storeId);
        }
        storeRepository.deleteById(storeId);
    }
}
