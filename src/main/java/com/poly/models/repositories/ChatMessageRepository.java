package com.poly.models.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.poly.models.entities.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
}
