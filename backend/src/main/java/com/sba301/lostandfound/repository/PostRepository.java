package com.sba301.lostandfound.repository;

import com.sba301.lostandfound.entity.Post;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    @Override
    @EntityGraph(attributePaths = {"user", "image", "location"})
    Optional<Post> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"user", "image", "location"})
    Page<Post> findAll(Specification<Post> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"user", "image", "location"})
    List<Post> findAll(Specification<Post> spec);

    @EntityGraph(attributePaths = {"user", "image", "location"})
    List<Post> findAllByIdIn(List<Long> ids);
}
