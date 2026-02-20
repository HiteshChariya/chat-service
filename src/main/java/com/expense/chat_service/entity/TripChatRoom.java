package com.expense.chat_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "trip_chat_room",
        schema = "conf",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "trip_id"),
                @UniqueConstraint(columnNames = "chat_room_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false, unique = true)
    private Long tripId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false, unique = true)
    private ChatRoom chatRoom;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
