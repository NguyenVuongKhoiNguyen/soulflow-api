package com.poly.models.mappers;

import java.time.LocalDateTime;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.poly.models.entities.Product;
import com.poly.models.entities.ProductImage;
import com.poly.models.repositories.ProductImageRepository;
import com.poly.models.repositories.ProductRepository;
import com.poly.models.requests.ProductImageRequest;
import com.poly.models.responses.ProductImageResponse;
import com.poly.models.services.ImageService;

import jakarta.persistence.EntityNotFoundException;

@Component
@Mapper(componentModel = "spring")
public abstract class ProductImageMapper { 
	
	@Autowired
	protected ProductImageRepository productImageRepo;
	
	@Autowired
	protected ProductRepository productRepo;

	@Autowired
	protected ImageService imageService;

	@Mapping(target = "createdDate",	ignore = true)
	@Mapping(target = "id",			source = "id")
	@Mapping(target = "product", 		ignore = true)
	@Mapping(target = "deleted", 		ignore = true)
	public abstract ProductImage toEntity(ProductImageRequest request);
	
	@Mapping(target = "createdDate", 				source = "createdDate", 			dateFormat = "dd-MM-yyyy HH:mm:ss")
	@Mapping(target = "id", 						source = "id")
	@Mapping(target = "productId", 					source = "product.id")
	@Mapping(target = "url", 						ignore = true)
	public abstract ProductImageResponse toResponse(ProductImage productImage); 
	
	public abstract List<ProductImage> toEntityList(List<ProductImageRequest> productImageRequest);
	
	public abstract List<ProductImageResponse> toResponseList(List<ProductImage> productImages);
	
	@AfterMapping
	protected void afterToEntity(ProductImageRequest request, @MappingTarget ProductImage productImage) {
		Long id = request.getId();
		if (id != null) {
			ProductImage oldProductImage = productImageRepo.findById(id)
					.orElseThrow(() -> new EntityNotFoundException("Product Image not found with id:" + id));
			productImage.setCreatedDate(oldProductImage.getCreatedDate());
			Product product = new Product();
			product.setId(request.getProductId());
			productImage.setProduct(product);
			return;
		} 

		productImage.setDeleted(false);
		productImage.setProduct(productRepo.findById(request.getProductId()).orElse(null));
		productImage.setCreatedDate(LocalDateTime.now());
	}

	@AfterMapping
	protected void afterToResponse(ProductImage productImage, @MappingTarget ProductImageResponse response) {
		try {
			String url = imageService.getPublicUrl(response.getName());
			response.setUrl(url);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
}
