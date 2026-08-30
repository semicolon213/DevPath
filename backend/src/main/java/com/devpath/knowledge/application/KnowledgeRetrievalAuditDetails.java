package com.devpath.knowledge.application;

public record KnowledgeRetrievalAuditDetails(
    String sourceTypes,
    int documentFilterCount,
    int resultCount,
    String policyVersion,
    String contextPurpose
) {}
