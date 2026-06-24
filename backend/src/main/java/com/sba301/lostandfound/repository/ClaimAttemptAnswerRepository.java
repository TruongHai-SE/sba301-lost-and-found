package com.sba301.lostandfound.repository;

import com.sba301.lostandfound.entity.ClaimAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimAttemptAnswerRepository
    extends JpaRepository<ClaimAttemptAnswer, Long> {
}
