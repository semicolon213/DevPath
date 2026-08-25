package com.devpath.audit.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuditRecordJpaRepository extends JpaRepository<AuditRecordJpaEntity, UUID> {
}
