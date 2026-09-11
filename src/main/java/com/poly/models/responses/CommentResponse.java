package com.poly.models.responses;

import java.util.List;

import lombok.Data;

@Data
public class CommentResponse {
    
    private String id;
    
    private String username;
    
    private String fullname;
    
    private String photo;

    private String url;
    
    private String content;
    
    private String createdDate;
    
    private String productId;
    
    private String accountId;
    
    private List<ReplyResponse> replyResponses;
}
