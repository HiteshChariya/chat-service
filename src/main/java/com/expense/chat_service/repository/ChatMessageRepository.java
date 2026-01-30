package com.expense.chat_service.repository;

import com.expense.chat_service.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId, Pageable pageable);
}
