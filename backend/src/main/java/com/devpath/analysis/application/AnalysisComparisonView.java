package com.devpath.analysis.application;

import java.util.List;

public record AnalysisComparisonView(List<AnalysisHistoryItemView> analyses) {
    public AnalysisComparisonView {
        analyses = List.copyOf(analyses);
        if (analyses.size() != 2) throw new IllegalArgumentException("analysis comparison requires two results");
    }
}
