package com.sba301.lostandfound.infrastructure.persistence.repository;

import com.sba301.lostandfound.infrastructure.persistence.entity.VerificationResponseJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationResponseJpaRepository
    extends JpaRepository<VerificationResponseJpaEntity, Long> {
}
