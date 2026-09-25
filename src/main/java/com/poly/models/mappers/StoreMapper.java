package com.poly.models.mappers;

import java.util.List;

import org.mapstruct.Mapper;

import com.poly.models.entities.Store;
import com.poly.models.requests.StoreRequest;
import com.poly.models.responses.StoreResponse;

@Mapper(componentModel = "spring")
public interface StoreMapper {

    Store toEntity(StoreRequest request);

    StoreResponse toResponse(Store store);

    List<StoreResponse> toResponseList(List<Store> stores);
}
