package com.expense.chat_service.security;

import com.expense.chat_service.request.Principle;
import lombok.Data;

@Data
public class ValidationResponse {
    private Boolean tokenValidated;
    private Principle user;
    private String failureMessage;
}
