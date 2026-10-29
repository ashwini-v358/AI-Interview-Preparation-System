package com.smartinterview.repository;

import com.smartinterview.entity.SkillGapAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SkillGapAnalysisRepository extends JpaRepository<SkillGapAnalysis, Long> {

    Optional<SkillGapAnalysis> findFirstByResumeIdAndJobDescriptionIdOrderByCreatedAtDesc(
            Long resumeId, Long jobDescriptionId);

    Optional<SkillGapAnalysis> findFirstByResumeUserIdOrderByAnalyzedAtDesc(Long userId);

    Optional<SkillGapAnalysis> findFirstByResumeIdOrderByAnalyzedAtDesc(Long resumeId);

    Optional<SkillGapAnalysis> findFirstByJobDescriptionIdOrderByAnalyzedAtDesc(Long jobDescriptionId);

    Optional<SkillGapAnalysis> findFirstByResumeIdAndJobDescriptionIdOrderByAnalyzedAtDesc(
            Long resumeId, Long jobDescriptionId);
}
