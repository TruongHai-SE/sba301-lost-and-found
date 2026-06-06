package com.sba301.lostandfound.repository;

import com.sba301.lostandfound.entity.MatchRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRequestRepository extends JpaRepository<MatchRequest, Long> {
}
