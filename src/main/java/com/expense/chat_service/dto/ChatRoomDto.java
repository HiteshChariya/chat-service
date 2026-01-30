package com.expense.chat_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDto {
    private Long id;
    private Long userId;
    private String userDisplayName;
    private String userEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChatMessageDto> messages;
    private Integer unreadCount; // for admin UI if needed
}
