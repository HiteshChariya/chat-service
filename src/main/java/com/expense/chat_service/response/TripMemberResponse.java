package com.expense.chat_service.response;

import lombok.Data;

@Data
public class TripMemberResponse {
    private Long id;
    private Long userId;
    private String email;
    private String displayName;
    private String role;
    private String status;
    private String joinedAt;
}
