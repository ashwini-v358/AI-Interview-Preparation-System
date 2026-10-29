package com.smartinterview.repository;

import com.smartinterview.entity.CandidateAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateAnswerRepository extends JpaRepository<CandidateAnswer, Long> {

    Optional<CandidateAnswer> findByInterviewQuestionId(Long interviewQuestionId);
}
