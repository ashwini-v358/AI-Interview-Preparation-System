package com.smartinterview.entity;

import com.smartinterview.entity.enums.SkillCategory;
import jakarta.persistence.*;
import lombok.*;

/**
 * Normalized catalog of skills across the industry.
 */
@Entity
@Table(name = "skills", indexes = {
    @Index(name = "idx_skill_normalized_name", columnList = "normalized_name", unique = true),
    @Index(name = "idx_skill_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Skill extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "normalized_name", nullable = false, unique = true, length = 100)
    private String normalizedName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private SkillCategory category = SkillCategory.OTHER;

    @Column(length = 255)
    private String description;

    @PrePersist
    @PreUpdate
    public void normalize() {
        if (this.name != null && this.normalizedName == null) {
            this.normalizedName = this.name.trim().toLowerCase();
        }
    }
}
