package com.sba301.lostandfound.service;

import com.sba301.lostandfound.dto.CreateStockImageRequest;
import com.sba301.lostandfound.dto.StockImageResponse;
import com.sba301.lostandfound.dto.UpdateStockImageRequest;
import com.sba301.lostandfound.entity.enums.Category;
import java.util.List;

public interface StockImageService {

    List<StockImageResponse> getAll();

    List<StockImageResponse> getByCategory(Category category);

    StockImageResponse getById(Long id);

    StockImageResponse create(CreateStockImageRequest request);

    StockImageResponse update(Long id, UpdateStockImageRequest request);

    void delete(Long id);
}
