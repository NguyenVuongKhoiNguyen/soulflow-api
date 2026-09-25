package com.poly.models.responses;

import lombok.Data;

@Data
public class AuthResponse {
	
	private String token;
	private boolean rememberMe;

	private String refreshToken;
	
	private long refreshMaxAge;
	
	/** Remaining token lifetime in seconds. */
	private long maxAge;
	
	private AccountResponse accountResponse;
}
