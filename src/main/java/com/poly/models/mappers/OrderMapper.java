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
import com.poly.models.entities.Order;
import com.poly.models.entities.OrderDetail;
import com.poly.models.entities.Payment;
import com.poly.models.enums.PaymentMethod;
import com.poly.models.repositories.OrderDetailRepository;
import com.poly.models.repositories.OrderRepository;
import com.poly.models.requests.OrderRequest;
import com.poly.models.responses.OrderResponse;

import jakarta.persistence.EntityNotFoundException;


@Component
@Mapper(componentModel = "spring", uses = {OrderDetailMapper.class})
public abstract class OrderMapper {
	
	@Autowired
	OrderRepository orderRepo;

	@Autowired
	OrderDetailRepository orderDetailRepo;

	@Mapping(target = "id", 		 			source = "id")
	@Mapping(target = "total", 		 			ignore = true)
	@Mapping(target = "createdDate", 			ignore = true)
	@Mapping(target = "expiredDate", 			ignore = true)
	@Mapping(target = "expired", 				ignore = true)
	@Mapping(target = "account", 	 			ignore = true)
	@Mapping(target= "deleted",					ignore = true)
	@Mapping(target = "payment", 				ignore = true)
	@Mapping(target = "orderDetails", 			source = "orderDetailRequests")
	public abstract Order toEntity(OrderRequest request);
	
	@Mapping(target = "createdDate", 	source = "createdDate", dateFormat = "dd-MM-yyyy HH:mm:ss")
	@Mapping(target = "id", 			source = "id")
	@Mapping(target = "expiredDate", 	source = "expiredDate", dateFormat = "dd-MM-yyyy HH:mm:ss")
	@Mapping(target = "total", 			source = "total", numberFormat = "#.##")
	@Mapping(target = "paymentMethod", expression = "java(order.getPayment() == null || order.getPayment().getPaymentMethod() == null ? null : order.getPayment().getPaymentMethod().name())")
	@Mapping(target = "accountId",			  	source = "account.id")
	@Mapping(target = "orderDetailResponses", 	source = "orderDetails")
	public abstract OrderResponse toResponse(Order order);
	
	public abstract List<OrderResponse> toResponseList(List<Order> orders);
	
	@AfterMapping
	protected void afterToEntity(OrderRequest request, @MappingTarget Order order) {
		
		Long id = order.getId();
		if (id != null) {

			orderDetailRepo.restoreProductQuantity(id);	
		
			Order oldOrder = orderRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Order not found with id: "+ id));
			order.setExpiredDate(oldOrder.getExpiredDate());
			order.setExpired(oldOrder.getExpired());
			order.setCreatedDate(oldOrder.getCreatedDate());
			order.setAccount(oldOrder.getAccount());
			order.setDeleted(oldOrder.getDeleted());
			order.calTotal();
			syncPayment(order, oldOrder.getPayment(), request.getPaymentMethod());
			return;
		}

		order.setCreatedDate(LocalDateTime.now());
		order.setExpiredDate(LocalDateTime.now().plusWeeks(2));
		order.setExpired(false);
		Account account = new Account();
		account.setId(request.getAccountId());
		order.setAccount(account);
		for (OrderDetail od : order.getOrderDetails()) {
			od.setOrder(order);
		}
		order.calTotal();
		syncPayment(order, null, request.getPaymentMethod());
		order.setDeleted(false);		
	}

	private void syncPayment(Order order, Payment existingPayment, PaymentMethod requestedMethod) {
		Payment payment = existingPayment == null ? new Payment() : existingPayment;
		PaymentMethod method = requestedMethod != null
			? requestedMethod
			: payment.getPaymentMethod() != null ? payment.getPaymentMethod() : PaymentMethod.COD;

		if (existingPayment == null || requestedMethod != null && requestedMethod != payment.getPaymentMethod()) {
			payment.setPaid(method != PaymentMethod.COD);
		}
		payment.setPaymentMethod(method);
		payment.setAmount(order.getTotal());
		if (payment.getPaymentDate() == null) {
			payment.setPaymentDate(LocalDateTime.now());
		}
		payment.setOrder(order);
		order.setPayment(payment);
	}
}
