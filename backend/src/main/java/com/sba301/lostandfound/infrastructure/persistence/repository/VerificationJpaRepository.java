package com.sba301.lostandfound.infrastructure.persistence.repository;

import com.sba301.lostandfound.infrastructure.persistence.entity.VerificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationJpaRepository extends JpaRepository<VerificationJpaEntity, Long> {
}
