package com.devpath.career.application;

public record CareerSummaryView(
    String careerId, String name, String localizedName, String status, String profileVersion, String purpose
) {}
