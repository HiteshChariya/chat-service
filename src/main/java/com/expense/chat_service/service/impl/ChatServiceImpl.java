package com.expense.chat_service.service.impl;

import com.expense.chat_service.dto.ChatMessageDto;
import com.expense.chat_service.dto.ChatRoomDto;
import com.expense.chat_service.entity.ChatMessage;
import com.expense.chat_service.entity.ChatRoom;
import com.expense.chat_service.repository.ChatMessageRepository;
import com.expense.chat_service.repository.ChatRoomRepository;
import com.expense.chat_service.request.Principle;
import com.expense.chat_service.response.BaseResponse;
import com.expense.chat_service.response.UserDataResponse;
import com.expense.chat_service.service.ChatService;
import com.expense.chat_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    private static final int DEFAULT_PAGE_SIZE = 50;

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomDto> getRooms(Principle principle) {
        if (isAdmin(principle)) {
            return chatRoomRepository.findAllByOrderByUpdatedAtDesc().stream()
                    .map(this::toRoomDto)
                    .collect(Collectors.toList());
        }
        // User: return only their room
        return chatRoomRepository.findByUserId(principle.getId())
                .map(List::of)
                .map(rooms -> rooms.stream().map(this::toRoomDto).collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public ChatRoomDto getRoom(Long roomId, Principle principle) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));
        if (!canAccessRoom(room, principle)) {
            throw new AccessDeniedException("You do not have access to this chat room");
        }
        ChatRoomDto dto = toRoomDto(room);
        dto.setMessages(room.getMessages().stream().map(this::toMessageDto).collect(Collectors.toList()));
        return dto;
    }

    @Override
    @Transactional
    public ChatRoomDto createRoom(Principle principle) {
        if (isAdmin(principle)) {
            throw new IllegalArgumentException("Admins cannot create a support room");
        }
        if (chatRoomRepository.existsByUserId(principle.getId())) {
            return getRoom(
                    chatRoomRepository.findByUserId(principle.getId()).orElseThrow().getId(),
                    principle
            );
        }
        ChatRoom room = ChatRoom.builder()
                .userId(principle.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        room = chatRoomRepository.save(room);
        return toRoomDto(room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessages(Long roomId, int page, int size, Principle principle) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));
        if (!canAccessRoom(room, principle)) {
            throw new AccessDeniedException("You do not have access to this chat room");
        }
        Pageable pageable = PageRequest.of(page, size > 0 ? size : DEFAULT_PAGE_SIZE);
        return chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(roomId, pageable)
                .getContent()
                .stream()
                .map(this::toMessageDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ChatMessageDto sendMessage(Long roomId, String content, Principle principle) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));
        if (!canAccessRoom(room, principle)) {
            throw new AccessDeniedException("You do not have access to this chat room");
        }
        String role = principle.getUserType() != null ? principle.getUserType() : "USER";
        String senderName = principle.getFirstName() != null
                ? principle.getFirstName() + " " + (principle.getLastName() != null ? principle.getLastName() : "")
                : principle.getEmail();
        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .senderId(principle.getId())
                .senderRole(role)
                .content(content)
                .senderDisplayName(senderName)
                .createdAt(LocalDateTime.now())
                .build();
        message = chatMessageRepository.save(message);
        room.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(room);

        ChatMessageDto dto = toMessageDto(message);
        dto.setSenderName(senderName);

        // Broadcast to all subscribers of this room
        messagingTemplate.convertAndSend("/topic/room/" + roomId, dto);
        return dto;
    }

    private boolean isAdmin(Principle principle) {
        return principle != null && "ADMIN".equalsIgnoreCase(principle.getUserType());
    }

    private boolean canAccessRoom(ChatRoom room, Principle principle) {
        if (principle == null) return false;
        if (isAdmin(principle)) return true;
        return room.getUserId().equals(principle.getId());
    }

    private ChatRoomDto toRoomDto(ChatRoom room) {
        try {
            BaseResponse<List<UserDataResponse>> response = userService.userDetailsById(room.getUserId());
            log.info("user detail : {}", response);
            UserDataResponse userDataResponses = response.getData().get(0);
            if (userDataResponses != null) {
                return ChatRoomDto.builder()
                        .id(room.getId())
                        .userId(room.getUserId())
                        .userDisplayName(userDataResponses.getFirstName() + " " + userDataResponses.getLastName())
                        .createdAt(room.getCreatedAt())
                        .updatedAt(room.getUpdatedAt())
                        .build();
            } else {
                return ChatRoomDto.builder()
                        .id(room.getId())
                        .userId(room.getUserId())
                        .createdAt(room.getCreatedAt())
                        .updatedAt(room.getUpdatedAt())
                        .build();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ChatMessageDto toMessageDto(ChatMessage msg) {
        return ChatMessageDto.builder()
                .id(msg.getId())
                .chatRoomId(msg.getChatRoom().getId())
                .senderId(msg.getSenderId())
                .senderRole(msg.getSenderRole())
                .senderName(msg.getSenderDisplayName())
                .content(msg.getContent())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
