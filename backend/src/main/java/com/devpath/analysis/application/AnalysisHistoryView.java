package com.devpath.analysis.application;

import java.util.List;

public record AnalysisHistoryView(
    List<AnalysisHistoryItemView> analyses,
    int limit,
    String nextCursor,
    long totalCount
) {
    public AnalysisHistoryView {
        analyses = List.copyOf(analyses);
    }
}
