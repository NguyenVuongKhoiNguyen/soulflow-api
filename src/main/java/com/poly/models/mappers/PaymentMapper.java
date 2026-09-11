package com.poly.models.mappers;

import java.time.LocalDateTime;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.poly.models.entities.Order;
import com.poly.models.entities.Payment;
import com.poly.models.requests.PaymentRequest;
import com.poly.models.responses.PaymentResponse;
import com.poly.models.enums.PaymentMethod;

@Mapper(componentModel = "spring", uses = {OrderMapper.class})
public abstract class PaymentMapper {


    @Mapping(target = "paymentDate", ignore = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "order", ignore = true)
    public abstract Payment toEntity(PaymentRequest request);

    @Mapping(target = "orderResponse", source = "order")
    @Mapping(target = "id", source = "id")
    @Mapping(target = "paymentMethod", expression = "java(payment.getPaymentMethod() == null ? null : payment.getPaymentMethod().name())")
    public abstract PaymentResponse toResponse(Payment payment);

    public abstract List<PaymentResponse> toResponseList(List<Payment> payments);

    @AfterMapping
    protected void afterToEntity(PaymentRequest request, @MappingTarget Payment payment) {
        payment.setPaymentDate(LocalDateTime.now());
        if (payment.getPaymentMethod() == null) {
            payment.setPaymentMethod(PaymentMethod.COD);
        }
        Order order = new Order();
        order.setId(request.getOrderId());
        payment.setOrder(order);
    }
}
