package com.expense.chat_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {
    @NotNull(message = "Chat room ID is required")
    private Long chatRoomId;

    @NotBlank(message = "Message content is required")
    private String content;
}
