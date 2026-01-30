package com.expense.chat_service.repository;

import com.expense.chat_service.entity.ChatRoomRead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomReadRepository extends JpaRepository<ChatRoomRead, Long> {

    Optional<ChatRoomRead> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);
}
