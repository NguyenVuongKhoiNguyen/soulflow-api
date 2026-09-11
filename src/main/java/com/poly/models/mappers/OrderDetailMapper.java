package com.poly.models.mappers;

import java.math.BigDecimal;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.poly.models.entities.OrderDetail;
import com.poly.models.entities.Product;
import com.poly.models.repositories.OrderDetailRepository;
import com.poly.models.repositories.ProductRepository;
import com.poly.models.requests.OrderDetailRequest;
import com.poly.models.responses.OrderDetailResponse;

import jakarta.persistence.EntityNotFoundException;


@Component
@Mapper(componentModel = "spring")
public abstract class OrderDetailMapper {
	
	@Autowired
	protected ProductRepository productRepo;

	@Autowired
	protected OrderDetailRepository orderDetailRepo;

	@Mapping(target = "name", 	ignore = true)
	@Mapping(target = "id", 		source = "id")
	@Mapping(target = "price", 	ignore = true)
	@Mapping(target = "subtotal", 	ignore = true)
	@Mapping(target = "product", 	ignore = true)
	@Mapping(target = "order", 	ignore = true)
	public abstract OrderDetail toEntity(OrderDetailRequest request);

	@Mapping(target = "price", 			source = "price", numberFormat = "#.##")
	@Mapping(target = "id", 			source = "id")
	@Mapping(target = "subtotal", 		source = "subtotal", numberFormat = "#.##")
	@Mapping(target = "productId" ,		expression = "java(orderDetail.getProduct() != null && orderDetail.getProduct().getId() != null ? String.valueOf(orderDetail.getProduct().getId()) : null)")
	@Mapping(target = "orderId" , 		expression = "java(orderDetail.getOrder() != null && orderDetail.getOrder().getId() != null ? String.valueOf(orderDetail.getOrder().getId()) : null)")
	public abstract OrderDetailResponse toResponse(OrderDetail orderDetail);

	public abstract List<OrderDetail> toEntityList(List<OrderDetailRequest> orderDetailRequests);

	public abstract List<OrderDetailResponse> toResponseList(List<OrderDetail> orderDetails);

	/* don't use builder when using after mapping */

	@AfterMapping
	protected void afterToEntity(OrderDetailRequest request, @MappingTarget OrderDetail orderDetail) {

		Long productId = request.getProductId();
		
		Product product = productRepo.findById(productId)
			.orElseThrow(() -> new EntityNotFoundException("Can't found product with ID: " + productId));

		Integer effectedRows = productRepo.decreaseQuantity(productId, request.getQuantity());

		if (effectedRows == 0) {
			throw new RuntimeException("Quantity is not enough in stock");
		}

		BigDecimal price = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
		Integer quantity = request.getQuantity() != null ? request.getQuantity() : 0;
		BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));

		orderDetail.setName(product.getName());
		orderDetail.setPrice(price);
		orderDetail.setSubtotal(subtotal);
		orderDetail.setProduct(product);
		productRepo.increaseSales(productId, quantity);
	}
}
