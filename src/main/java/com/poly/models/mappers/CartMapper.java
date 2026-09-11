package com.poly.models.mappers;

import java.time.LocalDateTime;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.poly.models.entities.Account;
import com.poly.models.entities.Cart;
import com.poly.models.entities.Item;
import com.poly.models.repositories.CartRepository;
import com.poly.models.requests.CartRequest;
import com.poly.models.responses.CartResponse;

import jakarta.persistence.EntityNotFoundException;

@Component
@Mapper(componentModel = "spring", uses = {ItemMapper.class})
public abstract class CartMapper {

	@Autowired
	private CartRepository cartRepo;

	@Mapping(target = "id", 				source = "id")
	@Mapping(target = "createdDate", 	ignore = true)
	@Mapping(target = "expiredDate", 	ignore = true)
	@Mapping(target = "expired", 		ignore = true)
	@Mapping(target = "total", 			ignore = true)
	@Mapping(target = "account", 		ignore = true)
	@Mapping(target = "deleted", 		ignore = true)
	@Mapping(target = "items", 			source = "request.itemRequests")
	public abstract Cart toEntity(CartRequest request);
	
	@Mapping(target = "createdDate", 	source = "createdDate", dateFormat = "dd-MM-yyyy HH:mm:ss")
	@Mapping(target = "id", 			source = "id")
	@Mapping(target = "total", 			source = "total", numberFormat = "#.##")
	@Mapping(target = "username", 		source = "account.username")
	@Mapping(target = "fullname", 		source = "account.fullname")
	@Mapping(target = "accountId", 		source = "account.id")
	@Mapping(target = "itemResponses", 	source = "items")
	public abstract CartResponse toResponse(Cart cart);
	
	public abstract List<CartResponse> toResponseList(List<Cart> carts);
	
	@AfterMapping
	protected void afterToEntity(CartRequest request, @MappingTarget Cart cart) {
		Long id = cart.getId();
		if (id != null) {
			Cart oldCart = cartRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Order not found with id: "+ id));
			cart.setCreatedDate(oldCart.getCreatedDate());
			cart.setExpiredDate(oldCart.getExpiredDate());
			cart.setExpired(oldCart.getExpired());
			cart.setDeleted(oldCart.getDeleted());
			cart.setAccount(oldCart.getAccount());
			cart.calTotal();
			return;
		}
		cart.setCreatedDate(LocalDateTime.now());
		cart.setExpiredDate(LocalDateTime.now().plusDays(5));
		cart.setExpired(false);
		Account account = new Account();
		account.setId(request.getAccountId());
		cart.setAccount(account);
		for (Item it : cart.getItems()) {
			it.setCart(cart);
		}
		cart.calTotal();
		cart.setDeleted(false);
	}
}
