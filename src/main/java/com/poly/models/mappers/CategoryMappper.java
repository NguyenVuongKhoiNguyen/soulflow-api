package com.poly.models.mappers;

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
import com.poly.models.repositories.CategoryRepository;
import com.poly.models.requests.CategoryRequest;
import com.poly.models.responses.CategoryResponse;

import jakarta.persistence.EntityNotFoundException;

@Component
@Mapper(componentModel = "spring", uses = {ProductMapper.class})
public abstract class CategoryMappper {
	
	@Autowired
	protected CategoryRepository categoryRepo;

	@Mapping(target = "id", 					source = "id")
	@Mapping(target = "products", 			ignore = true)
	@Mapping(target = "deleted", 			ignore = true)
	public abstract Category toEntity(CategoryRequest request); 
	
	@Mapping(target = "productResponses",	ignore = true)
	@Mapping(target = "id", 					source = "id")
	@Named("basicResponse")
	public abstract CategoryResponse toBasicResponse(Category category);

	@Mapping(target = "productResponses", 	source = "products",  qualifiedByName = "detailedProductResponseList")
	@Mapping(target = "id", 					source = "id")
	@Named("detailedResponse")
	public abstract CategoryResponse toDetailResponse(Category category);

	@IterableMapping(qualifiedByName =  "basicResponse")
	public abstract List<CategoryResponse> toBasicResponseList(List<Category> categories);

	@IterableMapping(qualifiedByName = "detailedResponse")
	public abstract List<CategoryResponse> toDetailResponseList(List<Category> categories);

	@AfterMapping
    protected void afterToEntity(@MappingTarget Category category) {
		Long id = category.getId();
		if (id != null) {
			Category oldCategory = categoryRepo.findById(id).
					orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));
			category.setDeleted(oldCategory.getDeleted());
			return;
		}
		category.setDeleted(false);
    }
}
