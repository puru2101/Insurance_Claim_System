package com.insurance.user.repository;

import com.insurance.user.entity.UserActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    Page<UserActivity> findByAuthUserIdOrderByCreatedAtDesc(Long authUserId, Pageable pageable);
}
