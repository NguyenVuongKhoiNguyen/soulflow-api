package com.poly.models.services;

import java.time.LocalDateTime;

import com.poly.models.enums.RoleCode;
import com.poly.models.enums.SortOrder;
import com.poly.models.requests.AccountRequest;
import com.poly.models.requests.AuthRequest;
import com.poly.models.responses.AccountResponse;
import com.poly.models.responses.AuthResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.services.impl.AccountServiceImpl.GoogleTokenDTO;

public interface AccountService {
	AuthResponse refresh(String refreshToken);
	AuthResponse login(AuthRequest authRequest);
	AuthResponse loginWithGoogle(GoogleTokenDTO googleToken);
	AccountResponse save(AccountRequest request);
	void softDeleteById(Long accountId);
	AccountResponse findById(Long accountId);
	AccountResponse findByUsername(String username);
	AccountResponse findByEmail(String email);
	AccountResponse findAccountDetailById(Long accountId);
	AccountResponse findAccountDetailByUsername(String username);
	AccountResponse findAccountDetailByEmail(String email);
	void checkAndExpireBeforePagination(
		Boolean deleted,
		String keyword,
		LocalDateTime fromDate,
		LocalDateTime toDate,
		Boolean disabled);
	PageResponse<AccountResponse> filterAndPaginateAccounts(
		Boolean deleted, 
		String keyword, 
		LocalDateTime fromDate, 
		LocalDateTime toDate, 
		Boolean disabled, 
		RoleCode role,
        SortOrder sortOrder, 
		Integer pageNumber, 
		Integer pageSize);
}
