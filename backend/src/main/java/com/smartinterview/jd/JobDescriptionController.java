package com.smartinterview.jd;

import com.smartinterview.dto.jd.JdSummaryResponse;
import com.smartinterview.dto.jd.JdUploadRequest;
import com.smartinterview.dto.jd.JdUploadResponse;
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
 * REST controller for Job Description upload and retrieval.
 *
 * Endpoints:
 *   POST  /api/v1/jd/upload  — submit JD text + trigger AI analysis
 *   GET   /api/v1/jd/my      — list current user's JDs
 *   GET   /api/v1/jd/{id}    — get full analysis for one JD
 */
@RestController
@RequestMapping("/api/v1/jd")
@RequiredArgsConstructor
@Tag(name = "Job Description", description = "Upload and AI-analyse job descriptions")
@SecurityRequirement(name = "bearerAuth")
public class JobDescriptionController {

    private final JobDescriptionService jdService;

    /** POST /api/v1/jd/upload */
    @PostMapping("/upload")
    @Operation(summary = "Upload a job description and trigger AI analysis")
    public ResponseEntity<JdUploadResponse> uploadJd(
            @Valid @RequestBody JdUploadRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        JdUploadResponse response = jdService.uploadAndAnalyze(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** GET /api/v1/jd/my */
    @GetMapping("/my")
    @Operation(summary = "List all job descriptions for the current user")
    public ResponseEntity<List<JdSummaryResponse>> getMyJds(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(jdService.getMyJds(userDetails.getUsername()));
    }

    /** GET /api/v1/jd/{id} */
    @GetMapping("/{id}")
    @Operation(summary = "Get full AI analysis for a specific job description")
    public ResponseEntity<JdUploadResponse> getJdDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(jdService.getJdDetail(id, userDetails.getUsername()));
    }
}
