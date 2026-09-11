package com.poly.models.mappers;

import java.time.LocalDateTime;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.poly.models.entities.Account;
import com.poly.models.entities.Role;
import com.poly.models.enums.RoleCode;
import com.poly.models.repositories.AccountRepository;
import com.poly.models.repositories.RoleRepository;
import com.poly.models.requests.AccountRequest;
import com.poly.models.responses.AccountResponse;
import com.poly.models.services.ImageService;

import jakarta.persistence.EntityNotFoundException;

@Mapper(componentModel = "spring", uses = {OrderMapper.class, CartMapper.class, RoleMapper.class, ChatMessageMapper.class})
public abstract class AccountMapper {
	
	@Autowired
	protected AccountRepository accountRepo;

	@Autowired
	protected RoleRepository roleRepo;
	
	@Autowired
	protected PasswordEncoder passwordEncoder;

	@Autowired
	protected ImageService imageService;

	@Mapping(target = "deleted",				ignore = true)
	@Mapping(target = "id",					source = "id")
	@Mapping(target = "createdDate", 			ignore = true)
	@Mapping(target = "credentialExpired", 		ignore = true)
	@Mapping(target = "credentialExpiredDate", 	ignore = true)
	@Mapping(target = "carts", 					ignore = true)
	@Mapping(target = "orders", 				ignore = true)
	@Mapping(target = "sentChatMessages",		ignore = true)
	@Mapping(target = "receivedChatMessages",	ignore = true)
	@Mapping(target = "roles",					source = "roleRequests")
	public abstract Account toEntity(AccountRequest request); 
	
	@Mapping(target = "createdDate", 				source = "createdDate", 			dateFormat = "dd-MM-yyyy HH:mm:ss")
	@Mapping(target = "id",						source = "id")
	@Mapping(target = "credentialExpiredDate", 		source = "credentialExpiredDate", 	dateFormat = "dd-MM-yyyy HH:mm:ss")
	@Mapping(target = "roleResponses", 				source = "roles")
	@Mapping(target = "url",						ignore = true)
	@Mapping(target = "cartResponses", 				ignore = true)
	@Mapping(target = "orderResponses", 			ignore = true)
	@Mapping(target = "sentChatMessageResponses", 	ignore = true)
	@Mapping(target = "receivedChatMessageResponses", ignore = true)
	@Named("basicResponse")
	public abstract AccountResponse toBasicResponse(Account account);

	@Mapping(target = "url", 					ignore = true)
	@Mapping(target = "id",						source = "id")
	@Mapping(target = "roleResponses", 			source = "roles")
	@Mapping(target = "cartResponses", 			source = "carts")
	@Mapping(target = "orderResponses", 		source = "orders")
	@Mapping(target = "sentChatMessageResponses",	source = "sentChatMessages")
	@Mapping(target = "receivedChatMessageResponses", source = "receivedChatMessages")
	@Named("detailedResponse")
	public abstract AccountResponse toDetailResponse(Account account);

	@IterableMapping(qualifiedByName = "basicResponse")
	public abstract List<AccountResponse> toBasicResponseList(List<Account> accounts);

	@IterableMapping(qualifiedByName = "detailedResponse")
	public abstract List<AccountResponse> toDetailedResponseList(List<Account> account); 
	
	@AfterMapping
    protected void afterToEntity(AccountRequest request, @MappingTarget Account account) {
		Long id = request.getId();
		if (id != null) { //update
			Account oldAccount = accountRepo.findById(id)
							.orElseThrow(() -> new EntityNotFoundException("Account not found with id: " + id));
			String password = request.getPassword();
			
			if (password != null) {
				account.setPassword(passwordEncoder.encode(request.getPassword()));
				account.setCredentialExpiredDate(LocalDateTime.now().plusMonths(3));
				account.setCredentialExpired(false);
			} else {
				account.setPassword(oldAccount.getPassword());
				account.setCredentialExpired(oldAccount.getCredentialExpired());
				account.setCredentialExpiredDate(oldAccount.getCredentialExpiredDate());
			}

			account.setCreatedDate(oldAccount.getCreatedDate());
			account.setDeleted(oldAccount.getDeleted());
			if (request.getPhoto() == null || request.getPhoto().isBlank()) {
				account.setPhoto(oldAccount.getPhoto());
			}
			account.setDisabled(request.getDisabled() == null ? false : request.getDisabled());
			if (account.getRoles() == null || account.getRoles().isEmpty()) {
				account.setRoles(oldAccount.getRoles());
			}
			return;
		}
		// new account — must have a password
		if (request.getPassword() != null && !request.getPassword().isBlank()) {
			account.setPassword(passwordEncoder.encode(request.getPassword()));
		} else {
			throw new IllegalArgumentException("Password is required for new accounts");
		}
		account.setDisabled(request.getDisabled() == null ? false : request.getDisabled());
        account.setCreatedDate(LocalDateTime.now());
		account.setCredentialExpiredDate(LocalDateTime.now().plusMonths(3));
		account.setCredentialExpired(false);
		account.setDeleted(false);
		if (account.getRoles() == null || account.getRoles().isEmpty()) {
			Role userRole = roleRepo.findByCode(RoleCode.USER)
					.orElseThrow(() -> new EntityNotFoundException("Default USER role not found"));
			account.setRoles(List.of(userRole));
		}
    }

	@AfterMapping
	protected void afterToResponse(@MappingTarget AccountResponse response) {
		try {
			String url = imageService.getPublicUrl(response.getPhoto());
			response.setUrl(url);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
}

