package com.devpath.recommendation.adapter.out.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

class RecommendationTemplateId implements Serializable {
    UUID policyId; UUID careerProfileVersionId; String category;
    public boolean equals(Object o){return o instanceof RecommendationTemplateId v && Objects.equals(policyId,v.policyId)&&Objects.equals(careerProfileVersionId,v.careerProfileVersionId)&&Objects.equals(category,v.category);}
    public int hashCode(){return Objects.hash(policyId,careerProfileVersionId,category);}
}
class RecommendationEvidenceId implements Serializable {
    UUID recommendationId; UUID evidenceId;
    public boolean equals(Object o){return o instanceof RecommendationEvidenceId v && Objects.equals(recommendationId,v.recommendationId)&&Objects.equals(evidenceId,v.evidenceId);}
    public int hashCode(){return Objects.hash(recommendationId,evidenceId);}
}
