package com.smartinterview.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * AnswerEvaluation entity storing AI evaluation metrics, feedback, and scoring
 * for a candidate's answer.
 */
@Entity
@Table(name = "answer_evaluations", indexes = {
    @Index(name = "idx_eval_answer_id", columnList = "candidate_answer_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_answer_id", nullable = false, unique = true)
    private CandidateAnswer candidateAnswer;

    @Column(name = "technical_accuracy_score")
    private Double technicalAccuracyScore;

    @Column(name = "completeness_score")
    private Double completenessScore;

    @Column(name = "clarity_score")
    private Double clarityScore;

    @Column(name = "overall_score", nullable = false)
    private Double overallScore;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Lob
    @Column(name = "areas_of_improvement", columnDefinition = "TEXT")
    private String areasOfImprovement;

    @Lob
    @Column(name = "detailed_feedback", columnDefinition = "LONGTEXT")
    private String detailedFeedback;

    @Column(name = "follow_up_topic", length = 200)
    private String followUpTopic;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;

    @PrePersist
    protected void onPrePersist() {
        if (this.evaluatedAt == null) {
            this.evaluatedAt = LocalDateTime.now();
        }
    }
}
