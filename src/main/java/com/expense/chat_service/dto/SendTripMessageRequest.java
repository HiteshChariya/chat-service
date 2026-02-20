package com.expense.chat_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendTripMessageRequest {

    @NotNull(message = "Trip ID is required")
    private Long tripId;

    @NotBlank(message = "Message content cannot be blank")
    private String content;
}
