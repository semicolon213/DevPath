package com.devpath.company.adapter.out.persistence;
import java.util.*;
import org.springframework.data.jpa.repository.*;
interface CompanyCatalogJpaRepository extends JpaRepository<CompanyJpaEntity,String> { @EntityGraph(attributePaths="activeProfile") List<CompanyJpaEntity> findByStatusOrderByLocalizedName(String status); @EntityGraph(attributePaths="activeProfile") Optional<CompanyJpaEntity> findByIdAndStatus(String id,String status); }
