package com.poly.seeding;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.poly.models.entities.Discount;
import com.poly.models.entities.Product;
import com.poly.models.repositories.DiscountRepository;
import com.poly.models.repositories.ProductRepository;

import lombok.RequiredArgsConstructor;

@Component
@Order(3)
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "app.discount-seeding.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class DiscountDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DiscountDataSeeder.class);
    
    private final DiscountRepository discountRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (discountRepository.count() > 0) {
            log.info("Discounts already seeded. Skipping discount seeding.");
            return;
        }

        List<Product> products = productRepository.findAll();
        Random random = new Random();

        int linkedProductsCount = 0;

        for (int i = 1; i <= 20; i++) {
            Discount discount = new Discount();
            discount.setCode("DISC_" + i + "_" + (10000 + random.nextInt(90000)));
            discount.setPercentage(BigDecimal.valueOf(random.nextInt(101))); // 0% to 100%
            discount.setDescription("Seeded discount " + i);
            discount.setCreatedDate(LocalDateTime.now());
            discount.setExpiredDate(LocalDateTime.now().plusMonths(1));
            discount.setExpired(false);
            discount.setDeleted(false);

            discount = discountRepository.save(discount);

            if (!products.isEmpty()) {
                // Link to 1-3 random products
                int numProductsToLink = 1 + random.nextInt(Math.min(3, products.size()));
                Collections.shuffle(products, random);
                
                for (int j = 0; j < numProductsToLink; j++) {
                    Product p = products.get(j);
                    // Add the discount to the product's list of discounts and save the product
                    // since Product is the owning side of the ManyToMany relationship
                    if (!p.getDiscounts().contains(discount)) {
                        p.getDiscounts().add(discount);
                        productRepository.save(p);
                        linkedProductsCount++;
                    }
                }
            }
        }
        
        log.info("Successfully seeded 20 discounts and linked them to {} products total.", linkedProductsCount);
    }
}
