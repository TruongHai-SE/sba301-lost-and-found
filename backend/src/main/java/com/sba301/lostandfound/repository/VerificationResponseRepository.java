package com.sba301.lostandfound.repository;

import com.sba301.lostandfound.entity.VerificationResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationResponseRepository
    extends JpaRepository<VerificationResponse, Long> {
}
