package com.poly.seeding;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.poly.models.entities.Store;
import com.poly.models.repositories.StoreRepository;

import lombok.RequiredArgsConstructor;

@Component
@Order(4)
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "app.store-seeding.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class StoreDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StoreDataSeeder.class);

    private static final List<StoreSeed> STORES = List.of(
        new StoreSeed(
            "SoulFlow District 1",
            "02838221555",
            "12 Nguyen Hue Street, Ben Nghe Ward, District 1, Ho Chi Minh City"
        ),
        new StoreSeed(
            "SoulFlow Phu Nhuan",
            "02838445333",
            "78 Phan Xich Long Street, Ward 2, Phu Nhuan District, Ho Chi Minh City"
        ),
        new StoreSeed(
            "SoulFlow Thu Duc",
            "02837224567",
            "45 Vo Van Ngan Street, Linh Chieu Ward, Thu Duc City, Ho Chi Minh City"
        )
    );

    private final StoreRepository storeRepository;

    @Override
    public void run(ApplicationArguments args) {
        int created = 0;
        for (StoreSeed seed : STORES) {
            Store store = storeRepository.findByNameIgnoreCase(seed.name())
                .orElseGet(() -> {
                    Store newStore = new Store();
                    newStore.setName(seed.name());
                    return newStore;
                });
            boolean isNew = store.getId() == null;
            store.setPhone(seed.phone());
            store.setAddress(seed.address());
            storeRepository.save(store);
            if (isNew) {
                created++;
            }
        }
        log.info("Store seed complete: {} stores created, {} stores available", created, STORES.size());
    }

    private record StoreSeed(String name, String phone, String address) {
    }
}
