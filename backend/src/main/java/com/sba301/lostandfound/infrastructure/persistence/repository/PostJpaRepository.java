package com.sba301.lostandfound.infrastructure.persistence.repository;

import com.sba301.lostandfound.infrastructure.persistence.entity.PostJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostJpaRepository extends JpaRepository<PostJpaEntity, Long> {
}
