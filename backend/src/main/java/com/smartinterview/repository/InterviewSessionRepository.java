package com.smartinterview.repository;

import com.smartinterview.entity.InterviewSession;
import com.smartinterview.entity.enums.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    List<InterviewSession> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<InterviewSession> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndStatus(Long userId, InterviewStatus status);

    long countByStatus(InterviewStatus status);
}
