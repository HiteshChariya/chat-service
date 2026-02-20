package com.expense.chat_service.service.impl;

import com.expense.chat_service.dto.ChatMessageDto;
import com.expense.chat_service.dto.ChatRoomDto;
import com.expense.chat_service.entity.ChatMessage;
import com.expense.chat_service.entity.ChatRoom;
import com.expense.chat_service.entity.ChatRoomRead;
import com.expense.chat_service.entity.TripChatRoom;
import com.expense.chat_service.repository.ChatMessageRepository;
import com.expense.chat_service.repository.ChatRoomReadRepository;
import com.expense.chat_service.repository.ChatRoomRepository;
import com.expense.chat_service.repository.TripChatRoomRepository;
import com.expense.chat_service.request.Principle;
import com.expense.chat_service.response.BaseResponse;
import com.expense.chat_service.response.UserDataResponse;
import com.expense.chat_service.service.ChatService;
import com.expense.chat_service.service.TripAccessService;
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
    private final ChatRoomReadRepository chatRoomReadRepository;
    private final TripChatRoomRepository tripChatRoomRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final TripAccessService tripAccessService;

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final long TRIP_ROOM_USER_ID_MULTIPLIER = -1L;

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomDto> getRooms(Principle principle) {
        List<ChatRoomDto> list;
        if (isAdmin(principle)) {
            list = chatRoomRepository.findAllByOrderByUpdatedAtDesc().stream()
                    .filter(room -> room.getUserId() != null && room.getUserId() > 0)
                    .map(this::toRoomDto)
                    .collect(Collectors.toList());
        } else {
            list = chatRoomRepository.findByUserId(principle.getId())
                    .map(List::of)
                    .map(rooms -> rooms.stream().map(this::toRoomDto).collect(Collectors.toList()))
                    .orElse(List.of());
        }
        list.forEach(dto -> dto.setUnreadCount(getUnreadCount(dto.getId(), principle)));
        return list;
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

    @Override
    @Transactional
    public void markRoomAsRead(Long roomId, Principle principle) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));
        if (!canAccessRoom(room, principle)) {
            throw new AccessDeniedException("You do not have access to this chat room");
        }
        LocalDateTime now = LocalDateTime.now();
        chatRoomReadRepository.findByChatRoomIdAndUserId(roomId, principle.getId())
                .ifPresentOrElse(
                        read -> {
                            read.setLastReadAt(now);
                            chatRoomReadRepository.save(read);
                        },
                        () -> chatRoomReadRepository.save(ChatRoomRead.builder()
                                .chatRoom(room)
                                .userId(principle.getId())
                                .lastReadAt(now)
                                .build())
                );
    }

    @Override
    @Transactional
    public List<ChatMessageDto> getTripMessages(Long tripId, int page, int size, Principle principle) {
        tripAccessService.requireTripMembership(tripId, principle);
        ChatRoom tripRoom = getOrCreateTripRoom(tripId);
        Pageable pageable = PageRequest.of(page, size > 0 ? size : DEFAULT_PAGE_SIZE);
        return chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(tripRoom.getId(), pageable)
                .getContent()
                .stream()
                .map(message -> toTripMessageDto(message, tripId))
                .toList();
    }

    @Override
    @Transactional
    public ChatMessageDto sendTripMessage(Long tripId, String content, Principle principle) {
        tripAccessService.requireTripMembership(tripId, principle);
        ChatRoom tripRoom = getOrCreateTripRoom(tripId);

        String role = principle.getUserType() != null ? principle.getUserType() : "USER";
        String senderName = principle.getFirstName() != null
                ? principle.getFirstName() + " " + (principle.getLastName() != null ? principle.getLastName() : "")
                : principle.getEmail();

        ChatMessage message = ChatMessage.builder()
                .chatRoom(tripRoom)
                .senderId(principle.getId())
                .senderRole(role)
                .content(content)
                .senderDisplayName(senderName)
                .createdAt(LocalDateTime.now())
                .build();
        message = chatMessageRepository.save(message);
        tripRoom.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(tripRoom);

        ChatMessageDto dto = toTripMessageDto(message, tripId);
        dto.setSenderName(senderName);
        messagingTemplate.convertAndSend("/topic/room/" + tripId, dto);
        return dto;
    }

    /** Sentinel for "never read" - valid timestamp in PostgreSQL (year 1); MIN would be out of range. */
    private static final LocalDateTime EPOCH_NEVER_READ = LocalDateTime.of(1, 1, 1, 0, 0, 0);

    /**
     * Count messages in the room sent by the other party (not the current user) that are after
     * the current user's last read timestamp. If they have never read, all messages from the other party count.
     */
    private int getUnreadCount(Long roomId, Principle principle) {
        Long currentUserId = principle.getId();
        LocalDateTime after = chatRoomReadRepository.findByChatRoomIdAndUserId(roomId, currentUserId)
                .map(ChatRoomRead::getLastReadAt)
                .orElse(EPOCH_NEVER_READ);
        return (int) chatMessageRepository.countByChatRoomIdAndSenderIdNotAndCreatedAtAfter(
                roomId, currentUserId, after);
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
        if (room.getUserId() == null || room.getUserId() <= 0) {
            return ChatRoomDto.builder()
                    .id(room.getId())
                    .userId(room.getUserId())
                    .userDisplayName("Trip Chat")
                    .createdAt(room.getCreatedAt())
                    .updatedAt(room.getUpdatedAt())
                    .build();
        }
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

    private ChatMessageDto toTripMessageDto(ChatMessage msg, Long tripId) {
        ChatMessageDto dto = toMessageDto(msg);
        dto.setChatRoomId(tripId);
        return dto;
    }

    private ChatRoom getOrCreateTripRoom(Long tripId) {
        return tripChatRoomRepository.findByTripId(tripId)
                .map(TripChatRoom::getChatRoom)
                .orElseGet(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    ChatRoom room = ChatRoom.builder()
                            .userId(TRIP_ROOM_USER_ID_MULTIPLIER * tripId)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    ChatRoom savedRoom = chatRoomRepository.save(room);

                    TripChatRoom tripChatRoom = TripChatRoom.builder()
                            .tripId(tripId)
                            .chatRoom(savedRoom)
                            .createdAt(now)
                            .build();
                    tripChatRoomRepository.save(tripChatRoom);
                    return savedRoom;
                });
    }
}
