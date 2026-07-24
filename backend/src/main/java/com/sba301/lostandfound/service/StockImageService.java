package com.sba301.lostandfound.service;

import com.sba301.lostandfound.dto.StockImageResponse;
import com.sba301.lostandfound.entity.enums.Category;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface StockImageService {

    List<StockImageResponse> getAll();

    List<StockImageResponse> getByCategory(Category category);

    StockImageResponse getById(Long id);

    StockImageResponse create(MultipartFile file, Category category, String label);

    StockImageResponse update(Long id, MultipartFile file, Category category, String label);

    void delete(Long id);
}