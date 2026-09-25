package com.poly.seeding;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.poly.models.entities.Account;
import com.poly.models.entities.Order;
import com.poly.models.entities.OrderDetail;
import com.poly.models.entities.Payment;
import com.poly.models.entities.Product;
import com.poly.models.entities.Store;
import com.poly.models.enums.PaymentMethod;
import com.poly.models.repositories.AccountRepository;
import com.poly.models.repositories.OrderRepository;
import com.poly.models.repositories.ProductRepository;
import com.poly.models.repositories.StoreRepository;

import lombok.RequiredArgsConstructor;

@Component
@org.springframework.core.annotation.Order(5)
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "app.order-seeding.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class OrderDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OrderDataSeeder.class);
    private static final int ORDER_COUNT = 100;
    private static final String LEGACY_ADDRESS_PREFIX = "Seed order delivery #";

    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Account> accounts = accountRepository.findAll(Sort.by("id").ascending()).stream()
            .filter(account -> !Boolean.TRUE.equals(account.getDeleted()))
            .toList();
        List<Product> products = productRepository.findAll(Sort.by("id").ascending()).stream()
            .filter(product -> !Boolean.TRUE.equals(product.getDeleted()))
            .toList();
        List<Store> stores = storeRepository.findAll(Sort.by("id").ascending());

        if (accounts.isEmpty()) {
            throw new IllegalStateException("Order seeding requires at least one existing account");
        }
        if (products.isEmpty()) {
            throw new IllegalStateException("Order seeding requires at least one existing product");
        }
        if (stores.isEmpty()) {
            throw new IllegalStateException("Order seeding requires at least one existing store");
        }

        Map<String, Order> existingByAddress = new HashMap<>();
        orderRepository.findByAddressStartingWithOrderByIdAsc(LEGACY_ADDRESS_PREFIX)
            .forEach(order -> existingByAddress.put(order.getAddress(), order));
        orderRepository.findByAddressIn(
                IntStream.rangeClosed(1, ORDER_COUNT)
                    .mapToObj(VietnameseAddressSeedData::forSeedNumber)
                    .distinct()
                    .toList()
            )
            .forEach(order -> existingByAddress.put(order.getAddress(), order));

        List<Order> ordersToSave = new ArrayList<>();
        List<Order> allSeededOrders = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int number = 1; number <= ORDER_COUNT; number++) {
            String legacyAddress = LEGACY_ADDRESS_PREFIX + String.format("%03d", number);
            String address = VietnameseAddressSeedData.forSeedNumber(number);
            Store store = stores.get((number - 1) % stores.size());
            Order existing = existingByAddress.get(legacyAddress);
            if (existing == null) {
                existing = existingByAddress.get(address);
            }
            if (existing != null) {
                existing.setAddress(address);
                existing.setStore(store);
                ordersToSave.add(existing);
                allSeededOrders.add(existing);
                continue;
            }

            Account account = accounts.get((number - 1) % accounts.size());
            Order order = new Order();
            order.setFullname(account.getFullname() == null ? "Seed Customer " + number : account.getFullname());
            order.setPhone(account.getPhone() == null ? "0900000000" : account.getPhone());
            order.setAddress(address);
            order.setShippingFee(BigDecimal.valueOf(ThreadLocalRandom.current().nextLong(50_001)));
            order.setCreatedDate(now.minusDays(ORDER_COUNT - number + 1L));
            order.setExpiredDate(now.plusYears(1));
            order.setExpired(false);
            order.setDeleted(false);
            order.setAccount(account);
            order.setStore(store);

            int detailCount = 1 + (number % Math.min(3, products.size()));
            List<OrderDetail> details = new ArrayList<>();
            for (int detailNumber = 0; detailNumber < detailCount; detailNumber++) {
                Product product = products.get((number - 1 + detailNumber) % products.size());
                int quantity = 1 + ((number + detailNumber) % 3);
                BigDecimal price = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();

                OrderDetail detail = new OrderDetail();
                detail.setName(product.getName());
                detail.setPrice(price);
                detail.setQuantity(quantity);
                detail.setSubtotal(price.multiply(BigDecimal.valueOf(quantity)));
                detail.setProduct(product);
                detail.setOrder(order);
                details.add(detail);
            }
            order.setOrderDetails(details);
            order.calTotal();

            Payment payment = new Payment();
            payment.setAmount(order.getTotal());
            payment.setPaid(true);
            payment.setPaymentMethod(number % 2 == 0 ? PaymentMethod.COD : PaymentMethod.E_BANKING);
            payment.setPaymentDate(order.getCreatedDate().plusHours(1));
            payment.setOrder(order);
            order.setPayment(payment);

            ordersToSave.add(order);
            allSeededOrders.add(order);
        }

        if (!ordersToSave.isEmpty()) {
            orderRepository.saveAllAndFlush(ordersToSave);
        }

        List<Long> seededIds = allSeededOrders.stream().map(Order::getId).toList();
        orderRepository.markSeededOrdersAsDelivered(seededIds);

        log.info(
            "Order seed complete: {} orders created or updated, {} seeded orders marked DELIVERED",
            ordersToSave.size(), seededIds.size()
        );
    }
}
