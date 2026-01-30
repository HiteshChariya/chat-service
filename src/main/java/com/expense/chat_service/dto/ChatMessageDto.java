package com.expense.chat_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;
    private Long chatRoomId;
    private Long senderId;
    private String senderRole; // USER, ADMIN
    private String senderName;
    private String content;
    private LocalDateTime createdAt;
}
