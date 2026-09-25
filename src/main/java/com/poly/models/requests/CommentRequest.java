package com.poly.models.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentRequest {
    private Long id;
    
    @NotBlank(message = "Content is required")
    private String content;
    
    private Long accountId;
    
    @NotNull(message = "Product ID is required")
    private Long productId;
}
