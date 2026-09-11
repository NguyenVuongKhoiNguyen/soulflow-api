package com.poly.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.poly.models.entities.Account;
import com.poly.models.entities.Role;
import com.poly.models.enums.RoleCode;
import com.poly.models.repositories.AccountRepository;
import com.poly.models.repositories.RoleRepository;

import lombok.RequiredArgsConstructor;

@Component
@Order(1)
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "app.account-seeding.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class AccountDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AccountDataSeeder.class);
    private static final int ACCOUNT_COUNT = 100;
    private static final String USERNAME_PREFIX = "user";

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.account-seeding.default-password:password}")
    private String defaultPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Role userRole = roleRepository.findByCode(RoleCode.USER)
            .orElseThrow(() -> new IllegalStateException(
                "Account seeding requires the USER role from the initial schema"
            ));

        Map<String, Account> existingByUsername = new HashMap<>();
        accountRepository.findByUsernameStartingWithOrderByUsernameAsc(USERNAME_PREFIX)
            .forEach(account -> existingByUsername.put(account.getUsername(), account));

        String encodedPassword = passwordEncoder.encode(defaultPassword);
        List<Account> accountsToSave = new ArrayList<>();
        int createdAccounts = 0;
        int addedRoleAssignments = 0;

        for (int number = 1; number <= ACCOUNT_COUNT; number++) {
            String suffix = String.format("%03d", number);
            String username = USERNAME_PREFIX + suffix;
            Account account = existingByUsername.get(username);

            if (account == null) {
                account = new Account();
                account.setUsername(username);
                account.setPassword(encodedPassword);
                account.setFullname("User " + suffix);
                account.setEmail(username + "@example.com");
                account.setPhone("0900000" + suffix);
                account.setAddress("Seed address " + number);
                account.setCreatedDate(LocalDateTime.now().plusSeconds(number));
                account.setCredentialExpiredDate(LocalDateTime.now().plusYears(10));
                account.setCredentialExpired(false);
                account.setDisabled(false);
                account.setDeleted(false);
                account.setRoles(new ArrayList<>());
                createdAccounts++;
            }

            boolean hasUserRole = account.getRoles().stream()
                .anyMatch(role -> role.getCode() == RoleCode.USER);
            if (!hasUserRole) {
                account.getRoles().add(userRole);
                addedRoleAssignments++;
            }
            accountsToSave.add(account);
        }

        accountRepository.saveAll(accountsToSave);
        log.info(
            "Account seed complete: {} accounts created, {} USER role assignments added",
            createdAccounts, addedRoleAssignments
        );
    }
}
