package com.sba301.lostandfound.repository;

import com.sba301.lostandfound.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
