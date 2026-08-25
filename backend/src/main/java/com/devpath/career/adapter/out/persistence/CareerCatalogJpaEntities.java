package com.devpath.career.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "careers")
class CareerJpaEntity {
    @Id @Column(name = "career_id") String id;
    @Column(name = "name", nullable = false) String name;
    @Column(name = "localized_name", nullable = false) String localizedName;
    @Column(name = "status", nullable = false) String status;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_profile_version_id") CareerProfileVersionJpaEntity activeProfile;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    protected CareerJpaEntity() {}
}

@Entity
@Table(name = "career_profile_versions")
class CareerProfileVersionJpaEntity {
    @Id @Column(name = "career_profile_version_id") UUID id;
    @Column(name = "career_id", nullable = false) String careerId;
    @Column(name = "version_label", nullable = false) String versionLabel;
    @Column(name = "status", nullable = false) String status;
    @Column(name = "purpose", nullable = false) String purpose;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "core_technologies", columnDefinition = "jsonb", nullable = false)
    List<String> coreTechnologies = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "required_competencies", columnDefinition = "jsonb", nullable = false)
    List<String> requiredCompetencies = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "preferred_competencies", columnDefinition = "jsonb", nullable = false)
    List<String> preferredCompetencies = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "evaluation_categories", columnDefinition = "jsonb", nullable = false)
    List<String> evaluationCategories = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "priority_weights", columnDefinition = "jsonb", nullable = false)
    Map<String, String> priorityWeights = new LinkedHashMap<>();
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "roadmap_template", columnDefinition = "jsonb", nullable = false)
    List<String> roadmapTemplate = new ArrayList<>();
    @Column(name = "effective_at", nullable = false) Instant effectiveAt;
    protected CareerProfileVersionJpaEntity() {}
}
