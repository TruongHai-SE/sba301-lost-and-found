package com.sba301.lostandfound.repository;

import com.sba301.lostandfound.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);

    Optional<User> findByMail(String mail);

    boolean existsByMail(String mail);

    boolean existsByPhone(String phone);

}
