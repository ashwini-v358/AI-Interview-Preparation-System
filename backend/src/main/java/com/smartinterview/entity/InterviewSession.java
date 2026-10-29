package com.smartinterview.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smartinterview.entity.enums.DifficultyLevel;
import com.smartinterview.entity.enums.InterviewStatus;
import com.smartinterview.entity.enums.InterviewType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * InterviewSession entity representing an active or completed interview simulation.
 */
@Entity
@Table(name = "interview_sessions", indexes = {
    @Index(name = "idx_interview_user_id", columnList = "user_id"),
    @Index(name = "idx_interview_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_description_id")
    private JobDescription jobDescription;

    @Column(name = "session_title", nullable = false, length = 200)
    private String sessionTitle;

    @Column(name = "target_role", length = 200)
    private String targetRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type", nullable = false, length = 40)
    @Builder.Default
    private InterviewType interviewType = InterviewType.COMPREHENSIVE_ADAPTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private InterviewStatus status = InterviewStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_difficulty", nullable = false, length = 20)
    @Builder.Default
    private DifficultyLevel currentDifficulty = DifficultyLevel.MEDIUM;

    @Column(name = "target_question_count", nullable = false)
    @Builder.Default
    private Integer targetQuestionCount = 5;

    @Column(name = "adaptive_mode", nullable = false)
    @Builder.Default
    private boolean adaptiveMode = false;

    @Column(name = "completed_question_count", nullable = false)
    @Builder.Default
    private Integer completedQuestionCount = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "interviewSession", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("questionOrder ASC")
    @Builder.Default
    private List<InterviewQuestion> questions = new ArrayList<>();

    @OneToOne(mappedBy = "interviewSession", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private InterviewReport report;

    @OneToOne(mappedBy = "interviewSession", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PreparationPlan preparationPlan;
}
