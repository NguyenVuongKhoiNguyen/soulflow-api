package com.poly.seeding;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.poly.models.entities.Account;
import com.poly.models.entities.Cart;
import com.poly.models.entities.Item;
import com.poly.models.entities.Product;
import com.poly.models.repositories.AccountRepository;
import com.poly.models.repositories.CartRepository;
import com.poly.models.repositories.ProductRepository;

import lombok.RequiredArgsConstructor;

@Component
@Order(6)
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "app.cart-seeding.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class CartDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CartDataSeeder.class);
    private static final int CART_COUNT = 100;
    private static final LocalDateTime SEED_EXPIRY_START = LocalDateTime.of(2099, 1, 1, 0, 1);
    private static final LocalDateTime SEED_EXPIRY_END = SEED_EXPIRY_START.plusMinutes(CART_COUNT);

    private final CartRepository cartRepository;
    private final AccountRepository accountRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Account> accounts = accountRepository.findAll(Sort.by("id").ascending()).stream()
            .filter(account -> !Boolean.TRUE.equals(account.getDeleted()))
            .toList();
        List<Product> products = productRepository.findAll(Sort.by("id").ascending()).stream()
            .filter(product -> !Boolean.TRUE.equals(product.getDeleted()))
            .toList();

        if (accounts.isEmpty()) {
            throw new IllegalStateException("Cart seeding requires at least one existing account");
        }
        if (products.isEmpty()) {
            throw new IllegalStateException("Cart seeding requires at least one existing product");
        }

        Map<LocalDateTime, Cart> existingByExpiry = new HashMap<>();
        cartRepository.findByExpiredDateBetweenOrderByIdAsc(SEED_EXPIRY_START, SEED_EXPIRY_END)
            .forEach(cart -> existingByExpiry.put(cart.getExpiredDate(), cart));

        List<Cart> cartsToCreate = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int number = 1; number <= CART_COUNT; number++) {
            LocalDateTime seedExpiry = SEED_EXPIRY_START.plusMinutes(number - 1L);
            if (existingByExpiry.containsKey(seedExpiry)) {
                continue;
            }

            Cart cart = new Cart();
            cart.setCreatedDate(now.minusDays(CART_COUNT - number + 1L));
            cart.setExpiredDate(seedExpiry);
            cart.setExpired(false);
            cart.setDeleted(false);
            cart.setAccount(accounts.get((number - 1) % accounts.size()));

            int itemCount = 1 + (number % Math.min(3, products.size()));
            List<Item> items = new ArrayList<>();
            for (int itemNumber = 0; itemNumber < itemCount; itemNumber++) {
                Product product = products.get((number - 1 + itemNumber) % products.size());
                int quantity = 1 + ((number + itemNumber) % 3);
                BigDecimal price = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();

                Item item = new Item();
                item.setQuantity(quantity);
                item.setSubtotal(price.multiply(BigDecimal.valueOf(quantity)));
                item.setProduct(product);
                item.setCart(cart);
                items.add(item);
            }

            cart.setItems(items);
            cart.calTotal();
            cartsToCreate.add(cart);
        }

        if (!cartsToCreate.isEmpty()) {
            cartRepository.saveAllAndFlush(cartsToCreate);
        }

        long seededCartCount = cartRepository
            .findByExpiredDateBetweenOrderByIdAsc(SEED_EXPIRY_START, SEED_EXPIRY_END)
            .stream()
            .filter(cart -> !cart.getExpiredDate().isAfter(SEED_EXPIRY_END.minusMinutes(1)))
            .count();

        log.info("Cart seed complete: {} carts created, {} seeded carts available", cartsToCreate.size(), seededCartCount);
    }
}
