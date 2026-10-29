package com.smartinterview.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smartinterview.entity.enums.SkillImportance;
import jakarta.persistence.*;
import lombok.*;

/**
 * Mapping between a Job Description and its required or preferred skills.
 */
@Entity
@Table(name = "job_required_skills", indexes = {
    @Index(name = "idx_jd_req_skill_jd", columnList = "job_description_id"),
    @Index(name = "idx_jd_req_skill_skill", columnList = "skill_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRequiredSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_description_id", nullable = false)
    private JobDescription jobDescription;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SkillImportance importance = SkillImportance.REQUIRED;

    @Column(name = "min_years_experience")
    private Double minYearsExperience;
}
