package com.sba301.lostandfound.service;

import com.sba301.lostandfound.dto.SearchResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service xử lý flow search: người mất đồ upload ảnh/text → tìm các post tương tự.
 *
 * <p>Search LUÔN trả về bản "preview" (BlurredPostSummary) - ảnh mờ, không có liên hệ.
 * Muốn xem ảnh rõ + liên hệ → phải đi qua flow claim.
 */
public interface SearchService {

    /**
     * Tìm các post tương tự dựa trên ảnh upload (và tuỳ chọn text mô tả).
     *
     * @param image       ảnh query (bắt buộc)
     * @param description text mô tả bổ sung (tuỳ chọn)
     * @param topK        số kết quả tối đa (default = 10)
     * @param targetType  LOST | FOUND | ALL (default = "FOUND" - vì người mất tìm bài "tìm thấy")
     */
    SearchResponse searchByImage(
        MultipartFile image,
        String description,
        Integer topK,
        String targetType
    );

    /**
     * Tìm các post tương tự dựa trên text (không cần ảnh).
     * Ít chính xác hơn image search, dùng khi user không có ảnh.
     */
    SearchResponse searchByText(String text, Integer topK, String targetType);
}
