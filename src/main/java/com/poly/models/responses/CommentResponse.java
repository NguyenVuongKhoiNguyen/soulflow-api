package com.poly.models.responses;

import java.util.List;

import lombok.Data;

@Data
public class CommentResponse {
    
    private String id;
    
    private String content;
    
    private String createdDate;

    private String accountId;

    private String accountUsername;
    
    private String accountFullname;
    
    private String accountPhoto;

    private String accountUrl;
    
    private String productId;

    private String productName;

    private List<ReplyResponse> replyResponses;
}
