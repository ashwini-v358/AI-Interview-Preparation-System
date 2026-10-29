package com.smartinterview.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * SkillGapAnalysis entity comparing candidate's Resume skills
 * against target Job Description requirements.
 */
@Entity
@Table(name = "skill_gap_analysis", indexes = {
    @Index(name = "idx_skill_gap_resume_jd", columnList = "resume_id, job_description_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillGapAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_description_id", nullable = false)
    private JobDescription jobDescription;

    @Column(name = "match_percentage", nullable = false)
    private Double matchPercentage;

    @Lob
    @Column(name = "matched_skills_json", columnDefinition = "LONGTEXT")
    private String matchedSkillsJson;

    @Lob
    @Column(name = "partial_skills_json", columnDefinition = "LONGTEXT")
    private String partialSkillsJson;

    @Lob
    @Column(name = "missing_skills_json", columnDefinition = "LONGTEXT")
    private String missingSkillsJson;

    @Lob
    @Column(name = "recommendations_json", columnDefinition = "LONGTEXT")
    private String recommendationsJson;

    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;

    @PrePersist
    protected void onPrePersist() {
        if (this.analyzedAt == null) {
            this.analyzedAt = LocalDateTime.now();
        }
    }
}
