package com.smartinterview.ai;

import java.util.List;

/**
 * Structured result of resume analysis by the AI service.
 *
 * @param extractedSkills   List of skill name strings extracted from the resume text.
 * @param experienceYears   Total years of professional experience detected.
 * @param summary           A concise 2-3 sentence professional summary of the candidate.
 * @param domains           List of domain areas (e.g., "Backend", "Cloud", "Machine Learning").
 * @param educationDetails  List of education entries detected (degree, institution, year).
 * @param rawAiResponse     The raw JSON string returned by the AI model (stored for audit).
 */
public record ResumeAnalysisResult(
        List<String> extractedSkills,
        double experienceYears,
        String summary,
        List<String> domains,
        List<String> educationDetails,
        String rawAiResponse
) {}
