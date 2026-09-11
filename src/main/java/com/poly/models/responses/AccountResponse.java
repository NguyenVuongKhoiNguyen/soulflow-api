package com.poly.models.responses;

import java.util.List;

import lombok.Data;

@Data
public class AccountResponse {
	
	private String id;
	
	private String username;
	
	private String fullname;
	
	private String email;
	
	private String photo;

	private String url;
	
	private String address;
	
	private String phone;
	
	private String createdDate;
	
	private String credentialExpiredDate;
	
	private String credentialExpired;
	
	private String disabled;

	private List<RoleResponse> roleResponses;

	private List<OrderResponse> orderResponses;
	
	private List<CartResponse> cartResponses;

	private List<ChatMessageResponse> sentChatMessageResponses;

	private List<ChatMessageResponse> receivedChatMessageResponses;
}
