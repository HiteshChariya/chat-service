package com.expense.chat_service.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDataResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String profile;
    private String mobile;
    private String password;
    private String email;
    private String token;
    private String role;

}