package com.poly.models.mappers;

import java.math.BigDecimal;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.poly.models.entities.Item;
import com.poly.models.entities.Product;
import com.poly.models.repositories.ProductRepository;
import com.poly.models.requests.ItemRequest;
import com.poly.models.responses.ItemResponse;

import jakarta.persistence.EntityNotFoundException;

@Component
@Mapper(componentModel = "spring")
public abstract class ItemMapper {
    
    @Autowired
    ProductRepository productRepo;

    @Mapping(target = "subtotal",       ignore = true)
    @Mapping(target = "id",             source = "id")
    @Mapping(target = "cart",           ignore = true)
    @Mapping(target = "product",        ignore = true)
    public abstract Item toEntity(ItemRequest request);

    @Mapping(target = "name",           source = "product.name")
    @Mapping(target = "id",             source = "id")
    @Mapping(target = "price",          source = "product.price", numberFormat = "#.##")
	@Mapping(target = "subtotal", 	    source = "subtotal", numberFormat = "#.##")
    @Mapping(target = "cartId",         source = "cart.id")
    @Mapping(target = "productId",      source = "product.id")
    public abstract ItemResponse toResponse(Item item);

    public abstract List<Item> toEntityList(List<ItemRequest> itemRequests);

    public abstract List<ItemResponse> toResponsesList(List<Item> items);

    @AfterMapping
    protected void afterToEntity(ItemRequest request, @MappingTarget Item item) {

        Long productId = request.getProductId();
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Can't find product with id: " + productId));
        item.setProduct(product);
        item.setSubtotal(product.getPrice().multiply(new BigDecimal(request.getQuantity())));
    }
}
