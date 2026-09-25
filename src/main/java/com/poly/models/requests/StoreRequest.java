package com.poly.models.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StoreRequest {

    private Long id;

    @NotBlank(message = "Store name is required")
    @Size(max = 100, message = "Store name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Store phone is required")
    @Size(max = 15, message = "Store phone cannot exceed 15 characters")
    @Pattern(regexp = "^[0-9+() .-]+$", message = "Store phone contains invalid characters")
    private String phone;

    @NotBlank(message = "Store address is required")
    @Size(max = 255, message = "Store address cannot exceed 255 characters")
    private String address;
}
