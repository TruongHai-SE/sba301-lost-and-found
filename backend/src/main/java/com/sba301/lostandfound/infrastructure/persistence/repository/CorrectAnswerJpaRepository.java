package com.sba301.lostandfound.infrastructure.persistence.repository;

import com.sba301.lostandfound.infrastructure.persistence.entity.CorrectAnswerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrectAnswerJpaRepository extends JpaRepository<CorrectAnswerJpaEntity, Long> {
}
