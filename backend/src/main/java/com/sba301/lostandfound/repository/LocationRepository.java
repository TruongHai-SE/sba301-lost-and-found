package com.sba301.lostandfound.repository;

import com.sba301.lostandfound.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {
}
