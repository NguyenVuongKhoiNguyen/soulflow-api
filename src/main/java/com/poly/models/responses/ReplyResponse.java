package com.poly.models.responses;

import lombok.Data;

@Data
public class ReplyResponse {
    
    private String id;
    
    private String content;
    
    private String createdDate;
    
    private String accountId;

    private String accountUsername;
    
    private String accountFullname;
    
    private String accountPhoto;

    private String accountUrl;

    private String commentProductId;

    private String commentProductName;
    
    private String commentId;

    private String commentAccountUsername;

    private String commentAccountFullname;

}