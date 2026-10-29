package com.smartinterview.config;

import com.smartinterview.entity.Skill;
import com.smartinterview.entity.User;
import com.smartinterview.entity.enums.Role;
import com.smartinterview.entity.enums.SkillCategory;
import com.smartinterview.repository.SkillRepository;
import com.smartinterview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Ensures baseline catalog data (default skills, test seed users)
 * is present upon application startup across all environments.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseDataInitializer {

    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        seedSkillsIfEmpty();
        seedDefaultUsersIfEmpty();
    }

    private void seedSkillsIfEmpty() {
        if (skillRepository.count() > 0) {
            log.info("Skills catalog already populated (count: {}).", skillRepository.count());
            return;
        }

        log.info("Seeding initial skills catalog into database...");
        List<Skill> initialSkills = List.of(
                Skill.builder().name("Java").normalizedName("java").category(SkillCategory.BACKEND).description("Core Java, Java 17/21, Streams, Multithreading").build(),
                Skill.builder().name("Spring Boot").normalizedName("spring boot").category(SkillCategory.BACKEND).description("Spring Boot 3, REST APIs, Microservices, Spring Data JPA").build(),
                Skill.builder().name("Python").normalizedName("python").category(SkillCategory.BACKEND).description("Python 3, FastAPI, Django, Data Analysis").build(),
                Skill.builder().name("Go").normalizedName("go").category(SkillCategory.BACKEND).description("Golang, High-throughput microservices, Goroutines").build(),
                Skill.builder().name("Node.js").normalizedName("node.js").category(SkillCategory.BACKEND).description("Node.js, Express, Event-driven architecture").build(),
                Skill.builder().name("React").normalizedName("react").category(SkillCategory.FRONTEND).description("React 18+, React Hooks, Functional Components, Virtual DOM").build(),
                Skill.builder().name("TypeScript").normalizedName("typescript").category(SkillCategory.FRONTEND).description("Static typing, interfaces, generics, TypeScript compiler").build(),
                Skill.builder().name("Next.js").normalizedName("next.js").category(SkillCategory.FRONTEND).description("Server-side rendering, React Server Components, App Router").build(),
                Skill.builder().name("HTML/CSS").normalizedName("html/css").category(SkillCategory.FRONTEND).description("HTML5 Semantic elements, Modern CSS3, Flexbox, Grid").build(),
                Skill.builder().name("MySQL").normalizedName("mysql").category(SkillCategory.DATABASE).description("MySQL 8, Relational Database, Indexing, Transactions, ACID").build(),
                Skill.builder().name("PostgreSQL").normalizedName("postgresql").category(SkillCategory.DATABASE).description("Advanced Relational Database, JSONB, Window Functions").build(),
                Skill.builder().name("MongoDB").normalizedName("mongodb").category(SkillCategory.DATABASE).description("NoSQL Document Store, Aggregation Pipeline, Scalability").build(),
                Skill.builder().name("Redis").normalizedName("redis").category(SkillCategory.DATABASE).description("In-memory cache, Pub/Sub, Data structures, High-speed storage").build(),
                Skill.builder().name("Docker").normalizedName("docker").category(SkillCategory.CLOUD_DEVOPS).description("Containerization, Dockerfile, Container orchestration basics").build(),
                Skill.builder().name("Kubernetes").normalizedName("kubernetes").category(SkillCategory.CLOUD_DEVOPS).description("Container orchestration, Pods, Services, Deployments").build(),
                Skill.builder().name("AWS").normalizedName("aws").category(SkillCategory.CLOUD_DEVOPS).description("Amazon Web Services, EC2, S3, RDS, Lambda, IAM").build(),
                Skill.builder().name("CI/CD Pipelines").normalizedName("ci/cd pipelines").category(SkillCategory.CLOUD_DEVOPS).description("GitHub Actions, GitLab CI, Automated Testing, Deployment").build(),
                Skill.builder().name("System Design").normalizedName("system design").category(SkillCategory.SYSTEM_DESIGN).description("Distributed systems, Microservices, Scalability, High availability").build(),
                Skill.builder().name("Data Structures & Algorithms").normalizedName("data structures & algorithms").category(SkillCategory.CS_FUNDAMENTALS).description("Arrays, Linked Lists, Trees, Graphs, Sorting, Dynamic Programming").build(),
                Skill.builder().name("Object-Oriented Programming").normalizedName("object-oriented programming").category(SkillCategory.CS_FUNDAMENTALS).description("OOP Principles, SOLID, Design Patterns, Clean Code").build(),
                Skill.builder().name("RESTful APIs").normalizedName("restful apis").category(SkillCategory.BACKEND).description("REST principles, HTTP methods, Status codes, OpenAPI/Swagger").build(),
                Skill.builder().name("WebSockets").normalizedName("websockets").category(SkillCategory.BACKEND).description("Full-duplex real-time communication, STOMP protocol").build(),
                Skill.builder().name("Communication").normalizedName("communication").category(SkillCategory.SOFT_SKILLS).description("Articulating technical thoughts clearly, active listening").build(),
                Skill.builder().name("Problem Solving").normalizedName("problem solving").category(SkillCategory.SOFT_SKILLS).description("Analytical thinking, debugging mindset, trade-off evaluation").build()
        );

        skillRepository.saveAll(initialSkills);
        log.info("Successfully seeded {} skills into database.", initialSkills.size());
    }

    private void seedDefaultUsersIfEmpty() {
        if (!userRepository.existsByEmail("admin@smartinterview.ai")) {
            User admin = User.builder()
                    .email("admin@smartinterview.ai")
                    .passwordHash(passwordEncoder.encode("Admin@123456"))
                    .fullName("Platform Administrator")
                    .role(Role.ROLE_ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            log.info("Created default administrator user: admin@smartinterview.ai");
        }

        if (!userRepository.existsByEmail("candidate@smartinterview.ai")) {
            User candidate = User.builder()
                    .email("candidate@smartinterview.ai")
                    .passwordHash(passwordEncoder.encode("Candidate@123456"))
                    .fullName("Alex Demo Candidate")
                    .role(Role.ROLE_CANDIDATE)
                    .enabled(true)
                    .build();
            userRepository.save(candidate);
            log.info("Created default candidate user: candidate@smartinterview.ai");
        }
    }
}
