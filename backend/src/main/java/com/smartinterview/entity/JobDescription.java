package com.smartinterview.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * JobDescription entity storing target job postings uploaded as PDF or text.
 */
@Entity
@Table(name = "job_descriptions", indexes = {
    @Index(name = "idx_jd_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDescription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "company_name", length = 150)
    private String companyName;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Lob
    @Column(name = "raw_text", columnDefinition = "LONGTEXT")
    private String rawText;

    @OneToOne(mappedBy = "jobDescription", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private JobDescriptionAnalysis analysis;

    @JsonIgnore
    @OneToMany(mappedBy = "jobDescription", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<JobRequiredSkill> requiredSkills = new ArrayList<>();
}
