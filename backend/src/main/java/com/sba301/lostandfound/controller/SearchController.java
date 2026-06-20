package com.sba301.lostandfound.controller;

import com.sba301.lostandfound.dto.ApiResponse;
import com.sba301.lostandfound.dto.SearchResponse;
import com.sba301.lostandfound.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoints phục vụ flow search: người dùng upload ảnh (hoặc text) để tìm các post tương tự.
 *
 * <p>Search LUÔN trả về BlurredPostSummary - ảnh mờ, không có thông tin liên hệ.
 * Để xem ảnh rõ → dùng POST /api/v1/posts/{postId}/claim.
 *
 *  - POST /api/v1/search (multipart): upload ảnh + tuỳ chọn text
 *  - POST /api/v1/search/text (json): search bằng text only
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * Search bằng ảnh (multipart/form-data).
     * Body:
     *  - image: file ảnh (bắt buộc)
     *  - description: text mô tả bổ sung (optional)
     *  - top_k: số kết quả tối đa (optional, default 10)
     *  - target_type: LOST | FOUND | ALL (optional, default FOUND)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SearchResponse>> searchByImage(
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "top_k", required = false) Integer topK,
            @RequestParam(value = "target_type", required = false) String targetType
    ) {
        SearchResponse result = searchService.searchByImage(image, description, topK, targetType);
        return ResponseEntity.ok(ApiResponse.success(result, "Search completed successfully"));
    }

    /**
     * Search bằng text (application/json hoặc form).
     * Body: { "text": "...", "top_k": 10, "target_type": "FOUND" }
     */
    @PostMapping(value = "/text", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<SearchResponse>> searchByText(@RequestBody SearchByTextRequest request) {
        SearchResponse result = searchService.searchByText(
                request.text(),
                request.topK(),
                request.targetType()
        );
        return ResponseEntity.ok(ApiResponse.success(result, "Search completed successfully"));
    }

    public record SearchByTextRequest(
            String text,
            Integer topK,
            String targetType
    ) {
    }
}
