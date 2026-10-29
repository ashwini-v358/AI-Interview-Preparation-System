package com.smartinterview.ai;

import java.util.List;

/**
 * Structured result of job description analysis by the AI service.
 *
 * @param requiredSkills    Skills explicitly marked as required or must-have.
 * @param niceToHaveSkills  Skills listed as preferred or bonus.
 * @param roleTitle         The extracted job title / role name.
 * @param experienceRequired Minimum years of experience mentioned.
 * @param domains           Domain areas the role belongs to.
 * @param rawAiResponse     Raw AI model response for audit purposes.
 */
public record JobDescriptionAnalysisResult(
        List<String> requiredSkills,
        List<String> niceToHaveSkills,
        String roleTitle,
        double experienceRequired,
        List<String> domains,
        String rawAiResponse
) {}
