package com.devpath.identity.adapter.out.persistence;

import com.devpath.identity.domain.CareerStage;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
class UserProfileJpaEntity {
    @Id @Column(name = "profile_id", nullable = false, updatable = false) private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false, unique = true) private UUID userId;
    @Enumerated(EnumType.STRING) @Column(name = "career_stage", length = 32) private CareerStage careerStage;
    @Column(name = "bio", length = 1000) private String bio;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(name = "version", nullable = false) private long version;
    protected UserProfileJpaEntity() {}
    UserProfileJpaEntity(UUID id, UUID userId, CareerStage careerStage, String bio, Instant createdAt, Instant updatedAt, long version) {
        this.id=id; this.userId=userId; this.careerStage=careerStage; this.bio=bio; this.createdAt=createdAt; this.updatedAt=updatedAt; this.version=version;
    }
    UUID id(){return id;} UUID userId(){return userId;} CareerStage careerStage(){return careerStage;}
    String bio(){return bio;} Instant createdAt(){return createdAt;} Instant updatedAt(){return updatedAt;} long version(){return version;}
}
