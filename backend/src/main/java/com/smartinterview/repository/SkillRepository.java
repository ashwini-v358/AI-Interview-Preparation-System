package com.smartinterview.repository;

import com.smartinterview.entity.Skill;
import com.smartinterview.entity.enums.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findByNameIgnoreCase(String name);

    Optional<Skill> findByNormalizedName(String normalizedName);

    List<Skill> findByCategory(SkillCategory category);

    boolean existsByNormalizedName(String normalizedName);
}
