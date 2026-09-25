package com.poly.models.requests;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AccountRequest {
    private Long id;
    
    @NotBlank(message = "Username is required")
	private String username;
    
    private String password;
    
    @NotBlank(message = "Fullname is required")
    private String fullname;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    private String email;
    
    private String photo;
    private String phone;
    private String address;
    private Boolean disabled;
    private List<RoleRequest> roleRequests;
}
