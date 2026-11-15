package com.iitsm.notification.repository;

import com.iitsm.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);
    boolean existsByDeduplicationKey(String deduplicationKey);
}
