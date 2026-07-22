package com.sba301.lostandfound.controller;

import com.sba301.lostandfound.dto.ApiResponse;
import com.sba301.lostandfound.dto.CreateStockImageRequest;
import com.sba301.lostandfound.dto.StockImageResponse;
import com.sba301.lostandfound.dto.UpdateStockImageRequest;
import com.sba301.lostandfound.entity.enums.Category;
import com.sba301.lostandfound.service.StockImageService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only CRUD endpoints cho quản lý ảnh mẫu.
 * Được bảo vệ bởi SecurityConfig rule: /api/v1/admin/** → hasRole("ADMIN").
 */
@RestController
@RequestMapping("/api/v1/admin/stock-images")
public class AdminStockImageController {

    private final StockImageService stockImageService;

    public AdminStockImageController(StockImageService stockImageService) {
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

    @PostMapping
    public ResponseEntity<ApiResponse<StockImageResponse>> create(
            @Valid @RequestBody CreateStockImageRequest request) {
        StockImageResponse result = stockImageService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), result, "Stock image created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StockImageResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStockImageRequest request) {
        StockImageResponse result = stockImageService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(result, "Stock image updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        stockImageService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Stock image deleted successfully"));
    }
}
