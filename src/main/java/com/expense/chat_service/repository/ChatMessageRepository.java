package com.expense.chat_service.repository;

import com.expense.chat_service.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId, Pageable pageable);

    /**
     * Count messages in a room sent by someone other than the given user, after the given timestamp.
     * Used for unread count: messages from the other party that the current user has not read.
     */
    long countByChatRoomIdAndSenderIdNotAndCreatedAtAfter(
            Long chatRoomId, Long excludeSenderId, java.time.LocalDateTime after);
}
