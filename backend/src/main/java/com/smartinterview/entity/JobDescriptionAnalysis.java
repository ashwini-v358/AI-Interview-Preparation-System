package com.smartinterview.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JobDescriptionAnalysis entity storing structured extraction insights
 * (required skills, nice-to-have skills, role scope, minimum experience).
 */
@Entity
@Table(name = "job_description_analysis", indexes = {
    @Index(name = "idx_jd_analysis_jd_id", columnList = "job_description_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDescriptionAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_description_id", nullable = false, unique = true)
    private JobDescription jobDescription;

    @Column(name = "role_title", length = 200)
    private String roleTitle;

    @Lob
    @Column(name = "required_skills_json", columnDefinition = "LONGTEXT")
    private String requiredSkillsJson;

    @Lob
    @Column(name = "nice_to_have_skills_json", columnDefinition = "LONGTEXT")
    private String niceToHaveSkillsJson;

    @Column(name = "minimum_experience_years")
    private Double minimumExperienceYears;

    @Lob
    @Column(name = "role_summary", columnDefinition = "TEXT")
    private String roleSummary;

    @Lob
    @Column(name = "raw_ai_response", columnDefinition = "LONGTEXT")
    private String rawAiResponse;

    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;

    @PrePersist
    protected void onPrePersist() {
        if (this.analyzedAt == null) {
            this.analyzedAt = LocalDateTime.now();
        }
    }
}
