package com.poly.models.mappers;

import java.time.LocalDateTime;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import com.poly.models.entities.Discount;
import com.poly.models.repositories.DiscountRepository;
import com.poly.models.requests.DiscountRequest;
import com.poly.models.responses.DiscountResponse;

import jakarta.persistence.EntityNotFoundException;

@Mapper(componentModel = "spring")
public abstract class DiscountMapper {
    
    @Autowired
    protected DiscountRepository discountRepo;

    @Mapping(target = "id",                 source = "id")
    @Mapping(target = "code",               ignore = true)
    @Mapping(target = "expired",            ignore = true)
    @Mapping(target = "createdDate",        ignore = true) 
    @Mapping(target = "deleted",          	ignore = true)
    @Mapping(target = "products",           ignore = true)
    public abstract Discount toEntity(DiscountRequest request); 

    @Mapping(target = "createdDate", 	    source = "createdDate",             dateFormat = "dd-MM-yyyy HH:mm:ss")
    @Mapping(target = "id",                 source = "id")
    @Mapping(target = "expiredDate", 	    source = "expiredDate",             dateFormat = "dd-MM-yyyy HH:mm:ss")
	@Mapping(target = "percentage", 		source = "percentage",              numberFormat = "#.##")
    @Mapping(target = "productResponses",   ignore = true)
    public abstract DiscountResponse toResponse(Discount discount);

    public abstract List<Discount> toEntityList(List<DiscountRequest> discountRequests);

    public abstract List<DiscountResponse> toResponseList(List<Discount> discounts);
    
    @AfterMapping
    protected void afterToEntity(DiscountRequest request, @MappingTarget Discount discount) {

        Long id = request.getId();
        LocalDateTime now = LocalDateTime.now();
        
        if (id != null) {
            Discount oldDiscount = discountRepo.findById(Long.valueOf(id))
                .orElseThrow(() -> new EntityNotFoundException("Discount not found with id: " + id));
            
            if (discount.getExpiredDate() != null) {
            	discount.setExpired(discount.getExpiredDate().isBefore(now));
    		} else {
    			discount.setExpiredDate(oldDiscount.getExpiredDate());
    			discount.setExpired(oldDiscount.getExpired());
    		}
  
            discount.setCode(oldDiscount.getCode());
            discount.setCreatedDate(oldDiscount.getCreatedDate());
            discount.setDeleted(oldDiscount.getDeleted());
            discount.setProducts(oldDiscount.getProducts());
            return;
        }
        
        if (discount.getExpiredDate() != null) {
        	discount.setExpired(discount.getExpiredDate().isBefore(now));
		} else {
			discount.setExpiredDate(null);
			discount.setExpired(false);
		}
      
        discount.setCode("D" + String.format("%06d", discountRepo.count() + 1));
        discount.setCreatedDate(now); 
        discount.setDeleted(false);
    }
}
