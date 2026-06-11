package com.sba301.lostandfound.repository;

import com.sba301.lostandfound.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByMail(String mail);

    boolean existsByMail(String mail);

    boolean existsByPhone(String phone);
}
