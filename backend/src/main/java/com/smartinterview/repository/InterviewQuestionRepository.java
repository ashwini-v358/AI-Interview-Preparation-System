package com.smartinterview.repository;

import com.smartinterview.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    List<InterviewQuestion> findByInterviewSessionIdOrderByQuestionOrderAsc(Long interviewSessionId);

    Optional<InterviewQuestion> findByInterviewSessionIdAndQuestionOrder(Long interviewSessionId, Integer questionOrder);
}
