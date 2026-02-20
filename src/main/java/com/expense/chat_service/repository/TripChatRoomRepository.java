package com.expense.chat_service.repository;

import com.expense.chat_service.entity.TripChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripChatRoomRepository extends JpaRepository<TripChatRoom, Long> {
    Optional<TripChatRoom> findByTripId(Long tripId);
}
