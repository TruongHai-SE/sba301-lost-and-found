package com.sba301.lostandfound.controller;

import com.sba301.lostandfound.dto.ApiResponse;
import com.sba301.lostandfound.dto.StockImageResponse;
import com.sba301.lostandfound.entity.enums.Category;
import com.sba301.lostandfound.service.StockImageService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints cho ảnh mẫu — tất cả user (kể cả chưa đăng nhập) có thể GET.
 */
@RestController
@RequestMapping("/api/v1/stock-images")
public class StockImageController {

    private final StockImageService stockImageService;

    public StockImageController(StockImageService stockImageService) {
        this.stockImageService = stockImageService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StockImageResponse>>> getAll(
            @RequestParam(required = false) Category category) {
        List<StockImageResponse> result = (category != null)
                ? stockImageService.getByCategory(category)
                : stockImageService.getAll();
        return ResponseEntity.ok(ApiResponse.success(result, "Stock images retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StockImageResponse>> getById(@PathVariable Long id) {
        StockImageResponse result = stockImageService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(result, "Stock image retrieved successfully"));
    }
}
