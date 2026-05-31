package com.sba301.lostandfound.infrastructure.persistence.repository;

import com.sba301.lostandfound.infrastructure.persistence.entity.MatchRequestJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRequestJpaRepository extends JpaRepository<MatchRequestJpaEntity, Long> {
}
