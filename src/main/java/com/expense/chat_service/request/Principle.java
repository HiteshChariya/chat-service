package com.expense.chat_service.request;

import lombok.Data;

@Data
public class Principle {

    private Long id;
    private String firstName;
    private String lastName;
    private String loginKey;
    private String loginSecret;
    private String email;
    private String userType;
    private String token;
    private String mobile;
    private String role;
    private boolean isActive;

}
