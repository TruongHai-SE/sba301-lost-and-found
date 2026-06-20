package com.sba301.lostandfound.repository;

import com.sba301.lostandfound.entity.User;
import com.sba301.lostandfound.entity.enums.UserType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByMail(String mail);

    boolean existsByMail(String mail);

    boolean existsByPhone(String phone);

    /**
     * Returns true if another user (different ID) already owns the given phone.
     */
    boolean existsByPhoneAndIdNot(String phone, Long id);

    /**
     * Paginated search with optional role filter.
     * When {@code search} is blank/null the LIKE condition always matches.
     * When {@code role} is null the type condition always matches.
     */
    @Query("""
            SELECT u FROM User u
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.mail) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:role IS NULL OR u.type = :role)
            """)
    Page<User> findBySearchAndRole(
            @Param("search") String search,
            @Param("role") UserType role,
            Pageable pageable);

    /**
     * Counts users with a specific role — used for last-admin guard.
     */
    long countByType(UserType type);

    /**
     * Checks whether the user has any posts — used before hard delete.
     */
    @Query("SELECT COUNT(p) > 0 FROM Post p WHERE p.user.id = :userId")
    boolean hasAnyPost(@Param("userId") Long userId);
}
