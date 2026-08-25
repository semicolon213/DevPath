package com.devpath.identity.adapter.out.persistence;

import com.devpath.identity.domain.PreferenceType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
class UserPreferenceJpaEntity {
    @Id @Column(name="preference_id", nullable=false, updatable=false) private UUID id;
    @Column(name="user_id", nullable=false, updatable=false) private UUID userId;
    @Enumerated(EnumType.STRING) @Column(name="preference_type", nullable=false, updatable=false, length=16) private PreferenceType type;
    @Column(name="selected_value", nullable=false, updatable=false, length=64) private String selectedValue;
    @Column(name="catalog_version", nullable=false, updatable=false, length=64) private String catalogVersion;
    @Column(name="active", nullable=false) private boolean active;
    @Column(name="selected_at", nullable=false, updatable=false) private Instant selectedAt;
    @Column(name="superseded_at") private Instant supersededAt;
    @Version @Column(name="version", nullable=false) private long version;
    protected UserPreferenceJpaEntity() {}
    UserPreferenceJpaEntity(UUID id, UUID userId, PreferenceType type, String selectedValue, String catalogVersion, boolean active, Instant selectedAt, Instant supersededAt, long version) {
        this.id=id; this.userId=userId; this.type=type; this.selectedValue=selectedValue; this.catalogVersion=catalogVersion; this.active=active; this.selectedAt=selectedAt; this.supersededAt=supersededAt; this.version=version;
    }
    UUID id(){return id;} UUID userId(){return userId;} PreferenceType type(){return type;} String selectedValue(){return selectedValue;}
    String catalogVersion(){return catalogVersion;}
    boolean active(){return active;} Instant selectedAt(){return selectedAt;} Instant supersededAt(){return supersededAt;} long version(){return version;}
}
