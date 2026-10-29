package com.smartinterview.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

/**
 * PreparationPlan entity providing a structured study roadmap
 * tailored to candidate's skill gaps and interview performance.
 */
@Entity
@Table(name = "preparation_plans", indexes = {
    @Index(name = "idx_plan_user_id", columnList = "user_id"),
    @Index(name = "idx_plan_session_id", columnList = "interview_session_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreparationPlan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_session_id")
    private InterviewSession interviewSession;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "plan_content_json", nullable = false, columnDefinition = "LONGTEXT")
    private String planContentJson;

    @Column(name = "duration_days")
    private Integer durationDays;
}
