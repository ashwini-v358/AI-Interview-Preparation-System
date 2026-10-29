package com.smartinterview.controller;

import com.smartinterview.dto.resume.ResumeUploadResponse;
import com.smartinterview.dto.resume.ResumeSummaryResponse;
import com.smartinterview.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for resume upload and retrieval.
 *
 * Endpoints:
 *   POST   /api/v1/resume/upload    - Upload PDF resume + trigger AI analysis (authenticated)
 *   GET    /api/v1/resume/my        - List the current user's resumes
 *   GET    /api/v1/resume/{id}      - Get full analysis for a specific resume
 */
@RestController
@RequestMapping("/api/v1/resume")
@RequiredArgsConstructor
@Tag(name = "Resume", description = "PDF resume upload, text extraction, and AI analysis")
@SecurityRequirement(name = "bearerAuth")
public class ResumeController {

    private final ResumeService resumeService;

    /**
     * POST /api/v1/resume/upload
     * Accepts a multipart/form-data PDF, runs the full pipeline:
     * store → extract → analyse, and returns the structured result.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a PDF resume and trigger AI analysis")
    public ResponseEntity<ResumeUploadResponse> uploadResume(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ResumeUploadResponse response = resumeService.uploadAndAnalyze(file, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/resume/my
     * Returns a list of all resumes uploaded by the current authenticated user.
     */
    @GetMapping("/my")
    @Operation(summary = "List all resumes uploaded by the current user")
    public ResponseEntity<List<ResumeSummaryResponse>> getMyResumes(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<ResumeSummaryResponse> resumes = resumeService.getMyResumes(userDetails.getUsername());
        return ResponseEntity.ok(resumes);
    }

    /**
     * GET /api/v1/resume/{id}
     * Returns the full upload + AI analysis details for a specific resume.
     * Returns 403 if the resume does not belong to the current user.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get full AI analysis for a specific resume")
    public ResponseEntity<ResumeUploadResponse> getResumeDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ResumeUploadResponse response = resumeService.getResumeDetail(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}
