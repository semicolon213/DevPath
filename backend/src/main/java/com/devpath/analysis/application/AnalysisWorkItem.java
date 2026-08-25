package com.devpath.analysis.application;

import com.devpath.analysis.domain.AnalysisJob;
import com.devpath.repository.domain.RepositorySnapshot;

record AnalysisWorkItem(AnalysisJob job, RepositorySnapshot snapshot) {}
