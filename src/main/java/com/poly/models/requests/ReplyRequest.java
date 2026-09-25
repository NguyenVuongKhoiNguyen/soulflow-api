package com.poly.models.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReplyRequest {
    private Long id;
    
    @NotBlank(message = "Content is required")
    private String content;
    
    @NotNull(message = "Comment ID is required")
    private Long commentId;
    
    private Long accountId;
}