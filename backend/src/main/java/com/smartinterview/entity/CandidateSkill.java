package com.smartinterview.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smartinterview.entity.enums.ProficiencyLevel;
import jakarta.persistence.*;
import lombok.*;

/**
 * Mapping between a candidate's Resume and identified skills,
 * including calculated proficiency and years of experience.
 */
@Entity
@Table(name = "candidate_skills", indexes = {
    @Index(name = "idx_cand_skill_resume", columnList = "resume_id"),
    @Index(name = "idx_cand_skill_skill", columnList = "skill_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(name = "proficiency_level", length = 30)
    @Builder.Default
    private ProficiencyLevel proficiencyLevel = ProficiencyLevel.INTERMEDIATE;

    @Column(name = "years_of_experience")
    private Double yearsOfExperience;

    @Column(name = "context_snippet", length = 500)
    private String contextSnippet;
}
