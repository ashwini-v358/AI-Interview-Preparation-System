package com.smartinterview.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ResumeAnalysis entity storing structured extraction insights
 * (skills, experience, education, domains) from AI evaluation.
 */
@Entity
@Table(name = "resume_analysis", indexes = {
    @Index(name = "idx_resume_analysis_resume_id", columnList = "resume_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false, unique = true)
    private Resume resume;

    @Lob
    @Column(name = "extracted_skills_json", columnDefinition = "LONGTEXT")
    private String extractedSkillsJson;

    @Column(name = "experience_years")
    private Double experienceYears;

    @Lob
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Lob
    @Column(name = "domains_json", columnDefinition = "TEXT")
    private String domainsJson;

    @Lob
    @Column(name = "education_json", columnDefinition = "TEXT")
    private String educationJson;

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
