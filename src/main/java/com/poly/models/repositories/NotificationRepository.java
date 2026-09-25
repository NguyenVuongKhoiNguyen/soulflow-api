package com.poly.models.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.poly.models.entities.AppNotification;

public interface NotificationRepository extends JpaRepository<AppNotification, Long> {

    @Modifying
    @Transactional
    @Query("UPDATE AppNotification notification SET notification.read = true WHERE notification.read = false")
    int markAllRead();
}
