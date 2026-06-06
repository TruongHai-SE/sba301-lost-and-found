package com.sba301.lostandfound.repository;

import com.sba301.lostandfound.entity.Verification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationRepository extends JpaRepository<Verification, Long> {
}
