package com.sba301.lostandfound.repository;

import com.sba301.lostandfound.entity.MatchRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRequestRepository extends JpaRepository<MatchRequest, Long> {

    /**
     * Checks whether the user has any match requests.
     * Used as a pre-delete guard: match_requests.user_id has no ON DELETE CASCADE.
     */
    @Query("SELECT COUNT(m) > 0 FROM MatchRequest m WHERE m.user.id = :userId")
    boolean existsByUserId(@Param("userId") Long userId);
}
