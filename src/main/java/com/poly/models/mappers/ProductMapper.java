package com.poly.models.mappers;

import java.time.LocalDateTime;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.poly.models.entities.Category;
import com.poly.models.entities.Product;
import com.poly.models.repositories.ProductRepository;
import com.poly.models.requests.ProductRequest;
import com.poly.models.responses.ProductResponse;

import jakarta.persistence.EntityNotFoundException;

@Component
@Mapper(componentModel = "spring", uses = {CommentMapper.class, ProductImageMapper.class, DiscountMapper.class})
public abstract class ProductMapper {
	
	@Autowired
	protected ProductRepository productRepo;
	
	@Mapping(target = "id", 					source = "id")
	@Mapping(target = "createdDate", 		ignore = true) 
	@Mapping(target = "sales", 				ignore = true)
	@Mapping(target = "category", 			ignore = true)
	@Mapping(target = "comments", 			ignore = true)
	@Mapping(target = "productImages", 		ignore = true) 
	@Mapping(target = "discounts", 			ignore = true)
	@Mapping(target = "deleted", 			ignore = true) 
	public abstract Product toEntity(ProductRequest request); 
	
	@Mapping(target = "price", 					source = "price", numberFormat = "#.##")
	@Mapping(target = "id", 					source = "id")
	@Mapping(target = "createdDate", 			dateFormat = "dd-MM-yyyy HH:mm:ss")
	@Mapping(target = "categoryId", 			source = "category.id")
	@Mapping(target = "commentResponses", 		ignore = true)
	@Mapping(target = "productImageResponses", 	source = "productImages")
	@Mapping(target = "discountResponses", 	ignore = true)
	@Named("basicResponse")
	public abstract ProductResponse toBasicResponse(Product product); 
	
	@Mapping(target = "createdDate", 			dateFormat = "dd-MM-yyyy")
	@Mapping(target = "id", 					source = "id")
	@Mapping(target = "categoryId", 			source = "category.id")
	@Mapping(target = "commentResponses",		source = "comments", 	qualifiedByName = "detailedCommentResponseList")
	@Mapping(target = "productImageResponses", 	source = "productImages")
	@Mapping(target = "discountResponses", 	source = "discounts")
	@Named("detailedResponse")
	public abstract ProductResponse toDetailedResponse(Product product);

	@Named("basicProductResponseList")
	@IterableMapping(qualifiedByName = "basicResponse")
	public abstract List<ProductResponse> toBasicResponseList(List<Product> products);

	@Named("detailedProductResponseList")
	@IterableMapping(qualifiedByName = "detailedResponse")
	public abstract List<ProductResponse> toDetailedResponseList(List<Product> products);


	@AfterMapping
	protected void afterToEntity(ProductRequest request, @MappingTarget Product product) {
		product.setAvailable(product.getQuantity() != null && product.getQuantity() > 0
			&& !Boolean.FALSE.equals(request.getAvailable()));
		Long id = product.getId();
		if (id != null) {
			Product oldProduct = productRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
			product.setCreatedDate(oldProduct.getCreatedDate());
			product.setSales(oldProduct.getSales());
			Category category = new Category();
			category.setId(Long.valueOf(request.getCategoryId()));
			product.setCategory(category);
			product.setDeleted(oldProduct.getDeleted());
			product.setDiscounts(oldProduct.getDiscounts());
			return;
		}
		product.setCreatedDate(LocalDateTime.now());
		product.setSales(Long.valueOf(0));
		Category category = new Category();
		category.setId(request.getCategoryId());
		product.setCategory(category);
		product.setDeleted(false);
	}
}
