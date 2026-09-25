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
import com.poly.models.entities.Store;
import com.poly.models.enums.PaymentMethod;
import com.poly.models.repositories.OrderDetailRepository;
import com.poly.models.repositories.OrderRepository;
import com.poly.models.repositories.StoreRepository;
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

    @Autowired
    StoreRepository storeRepo;

	@Autowired
	com.poly.models.services.QrService qrService;

	@Mapping(target = "id", 		 			source = "id")
	@Mapping(target = "total", 		 			ignore = true)
	@Mapping(target = "createdDate", 			ignore = true)
	@Mapping(target = "expiredDate", 			ignore = true)
	@Mapping(target = "expired", 				ignore = true)
	@Mapping(target = "account", 	 			ignore = true)
	@Mapping(target = "store", 				ignore = true)
	@Mapping(target= "deleted",					ignore = true)
	@Mapping(target = "payment", 				ignore = true)
	@Mapping(target = "orderDetails", 			source = "orderDetailRequests")
	public abstract Order toEntity(OrderRequest request);
	
	@Mapping(target = "createdDate", 	source = "createdDate", dateFormat = "dd-MM-yyyy HH:mm:ss")
	@Mapping(target = "id", 			source = "id")
	@Mapping(target = "expiredDate", 	source = "expiredDate", dateFormat = "dd-MM-yyyy HH:mm:ss")
	@Mapping(target = "total", 			source = "total", numberFormat = "#.##")
	@Mapping(target = "shippingFee", 	source = "shippingFee", numberFormat = "#.##")
	@Mapping(target = "paymentMethod", expression = "java(order.getPayment() == null || order.getPayment().getPaymentMethod() == null ? null : order.getPayment().getPaymentMethod().name())")
	@Mapping(target = "paid", expression = "java(order.getPayment() != null && Boolean.TRUE.equals(order.getPayment().getPaid()))")
	@Mapping(target = "accountId",			  	source = "account.id")
	@Mapping(target = "storeId", 				source = "store.id")
	@Mapping(target = "orderDetailResponses", 	source = "orderDetails")
	@Mapping(target = "paymentReference", ignore = true)
	@Mapping(target = "qrUrl", ignore = true)
	public abstract OrderResponse toResponse(Order order);

	@AfterMapping
	protected void addPaymentDetails(Order order, @MappingTarget OrderResponse response) {
		if (order.getId() != null && order.getPayment() != null
				&& order.getPayment().getPaymentMethod() == PaymentMethod.E_BANKING) {
			response.setPaymentReference("DH" + order.getId());
			if (!Boolean.TRUE.equals(order.getPayment().getPaid())
					&& !Boolean.TRUE.equals(order.getDeleted()) && !Boolean.TRUE.equals(order.getExpired())
					&& order.getStatus() == com.poly.models.enums.OrderStatus.PENDING
					&& (order.getExpiredDate() == null || order.getExpiredDate().isAfter(LocalDateTime.now()))
					&& order.getTotal() != null && order.getTotal().signum() > 0) {
				response.setQrUrl(qrService.buildQrUrl(order.getTotal(), response.getPaymentReference()));
			}
		}
	}
	
	public abstract List<OrderResponse> toResponseList(List<Order> orders);
	
	@AfterMapping
	protected void afterToEntity(OrderRequest request, @MappingTarget Order order) {
		if (order.getShippingFee() == null) {
			order.setShippingFee(java.math.BigDecimal.ZERO);
		}
		
		Long id = order.getId();
		if (id != null) {

			orderDetailRepo.restoreProductQuantity(id);	
		
			Order oldOrder = orderRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Order not found with id: "+ id));
			order.setExpiredDate(oldOrder.getExpiredDate());
			order.setExpired(oldOrder.getExpired());
			order.setCreatedDate(oldOrder.getCreatedDate());
			order.setAccount(oldOrder.getAccount());
			order.setStore(resolveStore(request.getStoreId(), oldOrder.getStore()));
			order.setDeleted(oldOrder.getDeleted());
			order.calTotal();
			syncPayment(order, oldOrder.getPayment(), request.getPaymentMethod());
			return;
		}

		order.setCreatedDate(LocalDateTime.now());
		order.setStatus(com.poly.models.enums.OrderStatus.PENDING);
		order.setExpiredDate(LocalDateTime.now().plusWeeks(2));
		order.setExpired(false);
		if (request.getAccountId() != null) {
			Account account = new Account();
			account.setId(request.getAccountId());
			order.setAccount(account);
		} else {
			order.setAccount(null);
		}
		order.setStore(resolveStore(request.getStoreId(), null));
		for (OrderDetail od : order.getOrderDetails()) {
			od.setOrder(order);
		}
		order.calTotal();
		syncPayment(order, null, request.getPaymentMethod());
		order.setDeleted(false);		
	}

    private Store resolveStore(Long storeId, Store existingStore) {
        if (storeId == null) {
            return existingStore;
        }
        return storeRepo.findById(storeId)
            .orElseThrow(() -> new EntityNotFoundException("Store not found with id: " + storeId));
    }

	private void syncPayment(Order order, Payment existingPayment, PaymentMethod requestedMethod) {
		Payment payment = existingPayment == null ? new Payment() : existingPayment;
		PaymentMethod method = requestedMethod != null
			? requestedMethod
			: payment.getPaymentMethod() != null ? payment.getPaymentMethod() : PaymentMethod.COD;

		if (existingPayment == null || requestedMethod != null && requestedMethod != payment.getPaymentMethod()) {
			payment.setPaid(method == PaymentMethod.IN_STORE);
		}
		payment.setPaymentMethod(method);
		payment.setAmount(order.getTotal());
		if (!Boolean.TRUE.equals(payment.getPaid())) {
			payment.setPaymentDate(null);
		} else if (payment.getPaymentDate() == null) {
			payment.setPaymentDate(LocalDateTime.now());
		}
		payment.setOrder(order);
		order.setPayment(payment);
	}
}
