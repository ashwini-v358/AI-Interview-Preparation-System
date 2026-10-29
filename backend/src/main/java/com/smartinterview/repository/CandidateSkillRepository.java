package com.smartinterview.repository;

import com.smartinterview.entity.CandidateSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateSkillRepository extends JpaRepository<CandidateSkill, Long> {

    List<CandidateSkill> findByResumeId(Long resumeId);

    void deleteByResumeId(Long resumeId);
}
