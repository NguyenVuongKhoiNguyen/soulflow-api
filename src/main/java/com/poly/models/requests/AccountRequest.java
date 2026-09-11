package com.poly.models.requests;

import java.util.List;

import lombok.Data;

@Data
public class AccountRequest {
    private Long id;
	private String username;
    private String password;
    private String fullname;
    private String email;
    private String photo;
    private String phone;
    private String address;
    private Boolean disabled;
    private List<RoleRequest> roleRequests;
}
