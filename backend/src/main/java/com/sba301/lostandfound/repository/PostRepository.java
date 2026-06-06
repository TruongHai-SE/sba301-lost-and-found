package com.sba301.lostandfound.repository;

import com.sba301.lostandfound.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
