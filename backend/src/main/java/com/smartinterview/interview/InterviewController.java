package com.smartinterview.interview;

import com.smartinterview.dto.interview.InterviewSessionResponse;
import com.smartinterview.dto.interview.InterviewReportResponse;
import com.smartinterview.dto.interview.AdaptiveProfileResponse;
import com.smartinterview.dto.interview.StartInterviewRequest;
import com.smartinterview.dto.interview.SubmitAnswerRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for interview session management.
 *
 * Endpoints:
 *   POST   /api/v1/interview/start             — start a new session
 *   GET    /api/v1/interview/my                 — list user's sessions
 *   GET    /api/v1/interview/{id}               — get session state
 *   POST   /api/v1/interview/{id}/answer/{qid}  — submit answer, get next Q
 */
@RestController
@RequestMapping("/api/v1/interview")
@RequiredArgsConstructor
@Tag(name = "Interview", description = "Adaptive interview simulation session management")
@SecurityRequirement(name = "bearerAuth")
public class InterviewController {

    private final InterviewService interviewService;
    private final InterviewReportService interviewReportService;

    /** POST /api/v1/interview/start */
    @PostMapping("/start")
    @Operation(summary = "Start a new adaptive interview session")
    public ResponseEntity<InterviewSessionResponse> startSession(
            @Valid @RequestBody StartInterviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        InterviewSessionResponse response = interviewService.startSession(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/adaptive/start")
    @Operation(summary = "Start an interview personalized from the user's previous performance")
    public ResponseEntity<InterviewSessionResponse> startAdaptiveSession(
            @Valid @RequestBody StartInterviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interviewService.startAdaptiveSession(request, userDetails.getUsername()));
    }

    @GetMapping("/adaptive/profile")
    @Operation(summary = "Get the authenticated user's performance profile and interview history")
    public ResponseEntity<AdaptiveProfileResponse> getAdaptiveProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(interviewService.getAdaptiveProfile(userDetails.getUsername()));
    }

    /** GET /api/v1/interview/my */
    @GetMapping("/my")
    @Operation(summary = "List all interview sessions for the current user")
    public ResponseEntity<List<InterviewSessionResponse>> getMySessions(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(interviewService.getMySessions(userDetails.getUsername()));
    }

    /** GET /api/v1/interview/{id} */
    @GetMapping("/{id}")
    @Operation(summary = "Get the current state of an interview session")
    public ResponseEntity<InterviewSessionResponse> getSession(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(interviewService.getSession(id, userDetails.getUsername()));
    }

    /** POST /api/v1/interview/{id}/answer/{questionId} */
    @PostMapping("/{id}/answer/{questionId}")
    @Operation(summary = "Submit answer to current question and receive next question + evaluation")
    public ResponseEntity<InterviewSessionResponse> submitAnswer(
            @PathVariable Long id,
            @PathVariable Long questionId,
            @Valid @RequestBody SubmitAnswerRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        InterviewSessionResponse response = interviewService.submitAnswer(id, questionId, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/report")
    @Operation(summary = "Generate or retrieve the completed interview report")
    public ResponseEntity<InterviewReportResponse> generateReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(interviewReportService.generateReport(id, userDetails.getUsername()));
    }

    @GetMapping("/{id}/report")
    @Operation(summary = "Get the current user's persisted interview report")
    public ResponseEntity<InterviewReportResponse> getReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(interviewReportService.getReport(id, userDetails.getUsername()));
    }
}
