package com.smartinterview.interview;

import com.smartinterview.dto.interview.AdaptiveReasonDto;
import com.smartinterview.entity.enums.DifficultyLevel;

import java.util.List;

public record AdaptiveQuestionPlan(
        String focusSkill,
        DifficultyLevel difficulty,
        List<AdaptiveReasonDto> reasons
) {}