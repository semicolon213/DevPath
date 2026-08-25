package com.devpath.career.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

interface CareerCatalogJpaRepository extends JpaRepository<CareerJpaEntity, String> {
    @EntityGraph(attributePaths = "activeProfile")
    List<CareerJpaEntity> findByStatusOrderByLocalizedName(String status);

    @EntityGraph(attributePaths = "activeProfile")
    Optional<CareerJpaEntity> findByIdAndStatus(String id, String status);

    @Query("select profile from CareerProfileVersionJpaEntity profile where profile.id = :id")
    Optional<CareerProfileVersionJpaEntity> findProfileById(@Param("id") UUID id);
}
