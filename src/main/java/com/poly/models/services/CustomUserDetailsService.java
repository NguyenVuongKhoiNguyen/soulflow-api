package com.poly.models.services;

import java.time.LocalDateTime;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.poly.models.entities.Account;
import com.poly.models.repositories.AccountRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final AccountRepository accountRepo;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException  {
		// TODO Auto-generated method stub
		
		accountRepo.checkAndExpireCredential(username);
		
		Account account = accountRepo.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("Account not found with username: " + username));

		System.out.println("Found in DB: " + account.getUsername());

		boolean credentialsExpired = account.getCredentialExpiredDate() != null
				&& account.getCredentialExpiredDate().isBefore(LocalDateTime.now());
		account.setCredentialExpired(credentialsExpired);
	
		return User.builder()
				.username(account.getUsername())
				.password(account.getPassword())
				.authorities(account.getRoles().stream()
						.map(role -> role.getCode().name())
						.toArray(String[]::new))
				.accountExpired(false)
				.accountLocked(false)
				.credentialsExpired(credentialsExpired)
				.disabled(Boolean.TRUE.equals(account.getDisabled()))
				.build();
	}

	/*
	when loading user from db
	must use custom user detail service
	*/
}
