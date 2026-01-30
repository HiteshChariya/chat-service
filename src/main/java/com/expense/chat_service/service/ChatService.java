package com.expense.chat_service.service;

import com.expense.chat_service.dto.ChatMessageDto;
import com.expense.chat_service.dto.ChatRoomDto;
import com.expense.chat_service.request.Principle;

import java.util.List;

public interface ChatService {

    /**
     * Get or create the support chat room for the current user (USER role).
     * Admins get all rooms.
     */
    List<ChatRoomDto> getRooms(Principle principle);

    /**
     * Get a single room by id. User can only access their own room; admin can access any.
     */
    ChatRoomDto getRoom(Long roomId, Principle principle);

    /**
     * Create a support room for the current user. Only USER role; one room per user.
     */
    ChatRoomDto createRoom(Principle principle);

    /**
     * Get messages for a room with pagination.
     */
    List<ChatMessageDto> getMessages(Long roomId, int page, int size, Principle principle);

    /**
     * Send a message and broadcast to room subscribers. Returns the saved message DTO.
     */
    ChatMessageDto sendMessage(Long roomId, String content, Principle principle);
}
