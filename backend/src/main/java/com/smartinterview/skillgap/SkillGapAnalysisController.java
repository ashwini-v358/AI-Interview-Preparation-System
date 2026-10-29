package com.smartinterview.skillgap;

import com.smartinterview.dto.skillgap.SkillGapAnalysisRequest;
import com.smartinterview.dto.skillgap.SkillGapAnalysisResponse;
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

@RestController
@RequestMapping("/api/v1/skill-gap")
@RequiredArgsConstructor
@Tag(name = "Skill Gap", description = "Compare analyzed resume skills with target job requirements")
@SecurityRequirement(name = "bearerAuth")
public class SkillGapAnalysisController {

    private final SkillGapAnalysisService skillGapAnalysisService;

    @PostMapping("/analyze")
    @Operation(summary = "Analyze the skill match between an owned resume and job description")
    public ResponseEntity<SkillGapAnalysisResponse> analyze(
            @Valid @RequestBody SkillGapAnalysisRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(skillGapAnalysisService.analyze(request, userDetails.getUsername()));
    }

    @GetMapping("/latest")
    @Operation(summary = "Get the latest persisted skill-gap analysis for an owned resume and job description")
    public ResponseEntity<SkillGapAnalysisResponse> getLatest(
            @RequestParam Long resumeId,
            @RequestParam Long jobDescriptionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(skillGapAnalysisService.getLatest(resumeId, jobDescriptionId, userDetails.getUsername()));
    }
}