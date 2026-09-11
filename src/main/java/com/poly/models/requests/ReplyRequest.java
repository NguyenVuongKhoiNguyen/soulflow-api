package com.poly.models.requests;

import lombok.Data;

@Data
public class ReplyRequest {
    private Long id;
    private String content;
    private Long commentId;
    private Long accountId;
}