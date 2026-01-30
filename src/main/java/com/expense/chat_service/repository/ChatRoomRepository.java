package com.expense.chat_service.repository;

import com.expense.chat_service.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<ChatRoom> findAllByOrderByUpdatedAtDesc();
}
