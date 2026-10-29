package com.smartinterview.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * InterviewReport entity containing comprehensive post-interview analysis,
 * strengths, weak areas, resume claim verifications, and preparation roadmap.
 */
@Entity
@Table(name = "interview_reports", indexes = {
    @Index(name = "idx_report_session_id", columnList = "interview_session_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_session_id", nullable = false, unique = true)
    private InterviewSession interviewSession;

    @Column(name = "overall_score", nullable = false)
    private Double overallScore;

    @Column(name = "technical_score")
    private Double technicalScore;

    @Column(name = "communication_score")
    private Double communicationScore;

    @Column(name = "problem_solving_score")
    private Double problemSolvingScore;

    @Lob
    @Column(name = "executive_summary", columnDefinition = "LONGTEXT")
    private String executiveSummary;

    @Lob
    @Column(name = "strengths_json", columnDefinition = "LONGTEXT")
    private String strengthsJson;

    @Lob
    @Column(name = "weaknesses_json", columnDefinition = "LONGTEXT")
    private String weaknessesJson;

    @Lob
    @Column(name = "claim_verifications_json", columnDefinition = "LONGTEXT")
    private String claimVerificationsJson;

    @Lob
    @Column(name = "preparation_roadmap_json", columnDefinition = "LONGTEXT")
    private String preparationRoadmapJson;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    protected void onPrePersist() {
        if (this.generatedAt == null) {
            this.generatedAt = LocalDateTime.now();
        }
    }
}
