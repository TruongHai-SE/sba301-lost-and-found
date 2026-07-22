package com.sba301.lostandfound.repository;

import com.sba301.lostandfound.entity.StockImage;
import com.sba301.lostandfound.entity.enums.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockImageRepository extends JpaRepository<StockImage, Long> {

    List<StockImage> findByCategory(Category category);
}
