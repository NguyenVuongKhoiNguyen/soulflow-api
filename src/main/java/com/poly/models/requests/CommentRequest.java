package com.poly.models.requests;

import lombok.Data;

@Data
public class CommentRequest {
    private Long id;
    private String content;
    private Long accountId;
    private Long productId;
}
