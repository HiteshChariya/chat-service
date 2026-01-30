package com.expense.chat_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message", schema = "conf")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    /**
     * USER or ADMIN
     */
    @Column(name = "sender_role", nullable = false, length = 20)
    private String senderRole;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sender_display_name", length = 255)
    private String senderDisplayName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
