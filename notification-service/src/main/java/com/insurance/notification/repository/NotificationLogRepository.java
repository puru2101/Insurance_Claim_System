package com.insurance.notification.repository;

import com.insurance.notification.entity.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    Page<NotificationLog> findByRecipientEmailOrderByCreatedAtDesc(String email, Pageable pageable);
    long countByStatus(NotificationLog.NotificationStatus status);
}
