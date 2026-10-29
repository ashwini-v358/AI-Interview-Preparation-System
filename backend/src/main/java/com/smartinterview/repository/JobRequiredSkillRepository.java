package com.smartinterview.repository;

import com.smartinterview.entity.JobRequiredSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRequiredSkillRepository extends JpaRepository<JobRequiredSkill, Long> {

    List<JobRequiredSkill> findByJobDescriptionId(Long jobDescriptionId);

    void deleteByJobDescriptionId(Long jobDescriptionId);
}
