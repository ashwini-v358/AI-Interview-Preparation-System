package com.smartinterview.repository;

import com.smartinterview.entity.JobDescriptionAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobDescriptionAnalysisRepository extends JpaRepository<JobDescriptionAnalysis, Long> {

    Optional<JobDescriptionAnalysis> findByJobDescriptionId(Long jobDescriptionId);
}
