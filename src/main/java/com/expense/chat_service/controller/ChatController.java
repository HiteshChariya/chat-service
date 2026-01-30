package com.expense.chat_service.controller;

import com.expense.chat_service.dto.ChatMessageDto;
import com.expense.chat_service.dto.ChatRoomDto;
import com.expense.chat_service.dto.SendMessageRequest;
import com.expense.chat_service.request.Principle;
import com.expense.chat_service.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDto>> getRooms(@AuthenticationPrincipal Principle principle) {
        return ResponseEntity.ok(chatService.getRooms(principle));
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ChatRoomDto> getRoom(@PathVariable Long roomId,
                                               @AuthenticationPrincipal Principle principle) {
        return ResponseEntity.ok(chatService.getRoom(roomId, principle));
    }

    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomDto> createRoom(@AuthenticationPrincipal Principle principle) {
        return ResponseEntity.ok(chatService.createRoom(principle));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal Principle principle) {
        return ResponseEntity.ok(chatService.getMessages(roomId, page, size, principle));
    }

    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ChatMessageDto> sendMessage(
            @PathVariable Long roomId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Principle principle) {
        String content = body != null ? body.get("content") : null;
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(chatService.sendMessage(roomId, content, principle));
    }

    /**
     * WebSocket endpoint: client sends to /app/chat.send with payload { chatRoomId, content }.
     * Principal is taken from message headers (set at CONNECT) so the payload binds only to SendMessageRequest.
     */
    @MessageMapping("/chat.send")
    public ChatMessageDto sendMessageViaWebSocket(@Payload @Valid SendMessageRequest request,
                                                  SimpMessageHeaderAccessor accessor) {
        Principle principle = getPrincipleFromAccessor(accessor);
        if (principle == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        }
        return chatService.sendMessage(request.getChatRoomId(), request.getContent(), principle);
    }

    private static Principle getPrincipleFromAccessor(SimpMessageHeaderAccessor accessor) {
        if (accessor == null || accessor.getUser() == null) return null;
        Object user = accessor.getUser();
        if (user instanceof UsernamePasswordAuthenticationToken auth) {
            Object p = auth.getPrincipal();
            if (p instanceof Principle pr) return pr;
        }
        return null;
    }
}
