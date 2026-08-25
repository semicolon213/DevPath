package com.devpath.rule.application;

import java.util.List;

public record SkillMatrixComparisonView(List<SkillMatrixView> matrices) {
    public SkillMatrixComparisonView {
        matrices = List.copyOf(matrices);
        if (matrices.size() != 2) {
            throw new IllegalArgumentException("a skill matrix comparison requires exactly two matrices");
        }
    }
}
