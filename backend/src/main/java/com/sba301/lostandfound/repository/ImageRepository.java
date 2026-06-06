package com.sba301.lostandfound.repository;

import com.sba301.lostandfound.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {
}
