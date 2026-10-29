package com.smartinterview.repository;

import com.smartinterview.dto.SchemaStatusResponse;
import com.smartinterview.entity.*;
import com.smartinterview.entity.enums.*;
import com.smartinterview.service.SchemaStatusService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class EntityRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ResumeAnalysisRepository resumeAnalysisRepository;

    @Autowired
    private JobDescriptionRepository jobDescriptionRepository;

    @Autowired
    private JobDescriptionAnalysisRepository jobDescriptionAnalysisRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private CandidateSkillRepository candidateSkillRepository;

    @Autowired
    private JobRequiredSkillRepository jobRequiredSkillRepository;

    @Autowired
    private SkillGapAnalysisRepository skillGapAnalysisRepository;

    @Autowired
    private InterviewSessionRepository interviewSessionRepository;

    @Autowired
    private InterviewQuestionRepository interviewQuestionRepository;

    @Autowired
    private CandidateAnswerRepository candidateAnswerRepository;

    @Autowired
    private AnswerEvaluationRepository answerEvaluationRepository;

    @Autowired
    private InterviewReportRepository interviewReportRepository;

    @Autowired
    private PreparationPlanRepository preparationPlanRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SchemaStatusService schemaStatusService;

    @Test
    @DisplayName("Should successfully persist, link, and query all Phase 2 entities end-to-end")
    void testCompleteEntityLifecycleAndRelationships() {
        // 1. User
        User candidate = User.builder()
                .email("test.candidate@test.com")
                .passwordHash("$2a$10$encryptedpasswordhash")
                .fullName("John Doe")
                .role(Role.ROLE_CANDIDATE)
                .enabled(true)
                .build();
        User savedCandidate = userRepository.save(candidate);
        assertThat(savedCandidate.getId()).isNotNull();
        assertThat(savedCandidate.getCreatedAt()).isNotNull();

        Optional<User> queriedCandidate = userRepository.findByEmail("test.candidate@test.com");
        assertThat(queriedCandidate).isPresent();
        assertThat(queriedCandidate.get().getFullName()).isEqualTo("John Doe");

        // 2. Resume & ResumeAnalysis
        Resume resume = Resume.builder()
                .user(savedCandidate)
                .fileName("john_doe_resume.pdf")
                .originalFileName("John_Doe_Resume_2026.pdf")
                .filePath("/uploads/resumes/john_doe_resume.pdf")
                .fileSize(102400L)
                .contentType("application/pdf")
                .rawText("Senior Full Stack Java Engineer with 5 years experience in Spring Boot, React, and MySQL.")
                .build();
        Resume savedResume = resumeRepository.save(resume);
        assertThat(savedResume.getId()).isNotNull();

        ResumeAnalysis resumeAnalysis = ResumeAnalysis.builder()
                .resume(savedResume)
                .extractedSkillsJson("[\"Java\", \"Spring Boot\", \"React\", \"MySQL\"]")
                .experienceYears(5.0)
                .summary("Experienced Java developer")
                .domainsJson("[\"FinTech\", \"E-Commerce\"]")
                .educationJson("[\"B.S. in Computer Science\"]")
                .rawAiResponse("{\"status\": \"success\"}")
                .analyzedAt(LocalDateTime.now())
                .build();
        ResumeAnalysis savedResumeAnalysis = resumeAnalysisRepository.save(resumeAnalysis);
        assertThat(savedResumeAnalysis.getId()).isNotNull();

        // 3. JobDescription & JobDescriptionAnalysis
        JobDescription jd = JobDescription.builder()
                .user(savedCandidate)
                .title("Senior Backend Engineer")
                .companyName("CloudScale AI")
                .fileName("senior_backend_jd.pdf")
                .filePath("/uploads/jds/senior_backend_jd.pdf")
                .rawText("Looking for a Senior Backend Engineer proficient in Java 21, Spring Boot, MySQL, and Docker.")
                .build();
        JobDescription savedJd = jobDescriptionRepository.save(jd);
        assertThat(savedJd.getId()).isNotNull();

        JobDescriptionAnalysis jdAnalysis = JobDescriptionAnalysis.builder()
                .jobDescription(savedJd)
                .roleTitle("Senior Backend Engineer")
                .requiredSkillsJson("[\"Java\", \"Spring Boot\", \"MySQL\"]")
                .niceToHaveSkillsJson("[\"Docker\", \"Kubernetes\"]")
                .minimumExperienceYears(4.0)
                .roleSummary("Lead backend microservices architecture")
                .rawAiResponse("{\"status\": \"success\"}")
                .analyzedAt(LocalDateTime.now())
                .build();
        JobDescriptionAnalysis savedJdAnalysis = jobDescriptionAnalysisRepository.save(jdAnalysis);
        assertThat(savedJdAnalysis.getId()).isNotNull();

        // 4. Skills & Mappings
        Skill javaSkill = skillRepository.findByNameIgnoreCase("Java")
                .orElseGet(() -> skillRepository.save(Skill.builder()
                        .name("Java")
                        .normalizedName("java")
                        .category(SkillCategory.BACKEND)
                        .description("Java programming language")
                        .build()));

        CandidateSkill candidateSkill = CandidateSkill.builder()
                .resume(savedResume)
                .skill(javaSkill)
                .proficiencyLevel(ProficiencyLevel.ADVANCED)
                .yearsOfExperience(5.0)
                .contextSnippet("5 years backend Java development")
                .build();
        candidateSkillRepository.save(candidateSkill);

        JobRequiredSkill jobRequiredSkill = JobRequiredSkill.builder()
                .jobDescription(savedJd)
                .skill(javaSkill)
                .importance(SkillImportance.REQUIRED)
                .minYearsExperience(3.0)
                .build();
        jobRequiredSkillRepository.save(jobRequiredSkill);

        // 5. SkillGapAnalysis
        SkillGapAnalysis gapAnalysis = SkillGapAnalysis.builder()
                .resume(savedResume)
                .jobDescription(savedJd)
                .matchPercentage(85.5)
                .matchedSkillsJson("[\"Java\", \"Spring Boot\", \"MySQL\"]")
                .partialSkillsJson("[\"Docker\"]")
                .missingSkillsJson("[\"Kubernetes\"]")
                .recommendationsJson("[\"Review Kubernetes deployment manifests\"]")
                .analyzedAt(LocalDateTime.now())
                .build();
        SkillGapAnalysis savedGap = skillGapAnalysisRepository.save(gapAnalysis);
        assertThat(savedGap.getId()).isNotNull();

        // 6. InterviewSession
        InterviewSession session = InterviewSession.builder()
                .user(savedCandidate)
                .resume(savedResume)
                .jobDescription(savedJd)
                .sessionTitle("Senior Backend Engineer Technical Simulation")
                .interviewType(InterviewType.COMPREHENSIVE_ADAPTIVE)
                .status(InterviewStatus.IN_PROGRESS)
                .currentDifficulty(DifficultyLevel.MEDIUM)
                .targetQuestionCount(5)
                .completedQuestionCount(1)
                .startedAt(LocalDateTime.now())
                .build();
        InterviewSession savedSession = interviewSessionRepository.save(session);
        assertThat(savedSession.getId()).isNotNull();

        // 7. InterviewQuestion, CandidateAnswer, and AnswerEvaluation
        InterviewQuestion question = InterviewQuestion.builder()
                .interviewSession(savedSession)
                .questionOrder(1)
                .questionText("How does Spring Boot handle transaction propagation in nested service calls?")
                .category("Technical Architecture")
                .targetSkill("Spring Boot")
                .difficultyLevel(DifficultyLevel.MEDIUM)
                .expectedKeyPointsJson("[\"REQUIRED vs REQUIRES_NEW\", \"Proxy mechanism\", \"Rollback behavior\"]")
                .createdAt(LocalDateTime.now())
                .build();
        InterviewQuestion savedQuestion = interviewQuestionRepository.save(question);
        assertThat(savedQuestion.getId()).isNotNull();

        CandidateAnswer answer = CandidateAnswer.builder()
                .interviewQuestion(savedQuestion)
                .answerText("Spring uses @Transactional proxies. Default propagation is REQUIRED which joins existing transaction, while REQUIRES_NEW suspends the current one.")
                .timeTakenSeconds(45)
                .submittedAt(LocalDateTime.now())
                .build();
        CandidateAnswer savedAnswer = candidateAnswerRepository.save(answer);
        assertThat(savedAnswer.getId()).isNotNull();

        AnswerEvaluation evaluation = AnswerEvaluation.builder()
                .candidateAnswer(savedAnswer)
                .technicalAccuracyScore(92.0)
                .completenessScore(88.0)
                .clarityScore(95.0)
                .overallScore(91.5)
                .strengths("Accurately differentiated REQUIRED and REQUIRES_NEW.")
                .areasOfImprovement("Could elaborate on rollback-for exceptions.")
                .detailedFeedback("Strong grasp of Spring Transaction management.")
                .followUpTopic("Distributed transaction handling using the Saga pattern.")
                .evaluatedAt(LocalDateTime.now())
                .build();
        AnswerEvaluation savedEval = answerEvaluationRepository.save(evaluation);
        assertThat(savedEval.getId()).isNotNull();

        // 8. InterviewReport and PreparationPlan
        InterviewReport report = InterviewReport.builder()
                .interviewSession(savedSession)
                .overallScore(89.0)
                .technicalScore(91.0)
                .communicationScore(87.0)
                .problemSolvingScore(89.0)
                .executiveSummary("Candidate demonstrated solid enterprise backend engineering skills.")
                .strengthsJson("[\"Spring Core\", \"Database Transaction Isolation\"]")
                .weaknessesJson("[\"Deep container orchestration\"]")
                .claimVerificationsJson("[\"Verified 5 years Java experience through detailed technical responses.\"]")
                .preparationRoadmapJson("[\"Deep dive into Kubernetes architecture\", \"Study Kafka consumer offset management\"]")
                .generatedAt(LocalDateTime.now())
                .build();
        InterviewReport savedReport = interviewReportRepository.save(report);
        assertThat(savedReport.getId()).isNotNull();

        PreparationPlan plan = PreparationPlan.builder()
                .user(savedCandidate)
                .interviewSession(savedSession)
                .title("7-Day Targeted Technical Interview Preparation Plan")
                .planContentJson("{\"day1\": \"Containerization Fundamentals\", \"day2\": \"Kubernetes Pods & Services\"}")
                .durationDays(7)
                .build();
        PreparationPlan savedPlan = preparationPlanRepository.save(plan);
        assertThat(savedPlan.getId()).isNotNull();

        // 9. AuditLog & Notification
        AuditLog auditLog = AuditLog.builder()
                .user(savedCandidate)
                .action("INTERVIEW_STARTED")
                .entityName("InterviewSession")
                .entityId(savedSession.getId())
                .ipAddress("127.0.0.1")
                .details("Session started with 5 target questions")
                .createdAt(LocalDateTime.now())
                .build();
        AuditLog savedAudit = auditLogRepository.save(auditLog);
        assertThat(savedAudit.getId()).isNotNull();

        Notification notification = Notification.builder()
                .user(savedCandidate)
                .title("Interview Report Ready")
                .message("Your performance report for Senior Backend Engineer is now available.")
                .isRead(false)
                .notificationType(NotificationType.REPORT_READY)
                .createdAt(LocalDateTime.now())
                .build();
        Notification savedNotif = notificationRepository.save(notification);
        assertThat(savedNotif.getId()).isNotNull();

        // 10. Verify SchemaStatusService
        SchemaStatusResponse status = schemaStatusService.getSchemaStatus();
        assertThat(status.isJpaInitialized()).isTrue();
        assertThat(status.getTotalUsers()).isGreaterThanOrEqualTo(1);
        assertThat(status.getTotalResumes()).isGreaterThanOrEqualTo(1);
        assertThat(status.getTotalJobDescriptions()).isGreaterThanOrEqualTo(1);
        assertThat(status.getTotalSkills()).isGreaterThanOrEqualTo(20);
        assertThat(status.getTotalInterviewSessions()).isGreaterThanOrEqualTo(1);
        assertThat(status.getEntityCounts()).containsKey("answer_evaluations");
    }
}
