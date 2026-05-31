package com.sba301.lostandfound.infrastructure.persistence.repository;

import com.sba301.lostandfound.infrastructure.persistence.entity.ImageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageJpaRepository extends JpaRepository<ImageJpaEntity, Long> {
}
