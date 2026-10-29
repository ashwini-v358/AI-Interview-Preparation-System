package com.smartinterview.preparation;

import com.smartinterview.dto.preparation.PreparationPlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/preparation-plan")
@RequiredArgsConstructor
@Tag(name = "Preparation Plan", description = "Generate and retrieve a personalized skill-gap learning roadmap")
@SecurityRequirement(name = "bearerAuth")
public class PreparationPlanController {

    private final PreparationPlanService preparationPlanService;

    @GetMapping
    @Operation(summary = "Get the authenticated user's latest preparation plan")
    public ResponseEntity<PreparationPlanResponse> getCurrent(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(preparationPlanService.getCurrent(userDetails.getUsername()));
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate and persist a preparation plan from the user's latest skill-gap analysis")
    public ResponseEntity<PreparationPlanResponse> generate(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(preparationPlanService.generate(userDetails.getUsername()));
    }
}