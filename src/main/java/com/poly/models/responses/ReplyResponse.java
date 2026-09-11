package com.poly.models.responses;

import lombok.Data;

@Data
public class ReplyResponse {
    
    private String id;
    
    private String username;
    
    private String fullname;
    
    private String photo;
    
    private String content;
    
    private String createdDate;
    
    private String accountId;
    
    private String commentId;
}