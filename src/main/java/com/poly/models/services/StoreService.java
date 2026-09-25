package com.poly.models.services;

import java.util.List;

import com.poly.models.requests.StoreRequest;
import com.poly.models.responses.PageResponse;
import com.poly.models.responses.StoreResponse;

public interface StoreService {

    StoreResponse save(StoreRequest request);

    StoreResponse findById(Long storeId);

    List<StoreResponse> findAll();

    PageResponse<StoreResponse> filterAndPaginateStores(Long id, String storeName, Integer pageNumber, Integer pageSize);

    void deleteById(Long storeId);
}
