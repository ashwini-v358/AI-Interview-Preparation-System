package com.smartinterview.readiness;

import com.smartinterview.dto.readiness.ReadinessResponse;
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
@RequestMapping("/api/v1/readiness")
@RequiredArgsConstructor
@Tag(name = "Readiness", description = "Track interview readiness and get personalized recommendations")
@SecurityRequirement(name = "bearerAuth")
public class ReadinessController {

    private final ReadinessService readinessService;

    @GetMapping
    @Operation(summary = "Get current readiness assessment for the authenticated user")
    public ResponseEntity<ReadinessResponse> getReadiness(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(readinessService.getReadiness(userDetails.getUsername()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh readiness assessment based on latest data")
    public ResponseEntity<ReadinessResponse> refreshReadiness(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(readinessService.refresh(userDetails.getUsername()));
    }
}
