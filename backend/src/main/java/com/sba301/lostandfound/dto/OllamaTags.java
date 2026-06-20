package com.sba301.lostandfound.dto;

import java.util.List;

/**
 * Output đã được parse từ {@link VisionDescription}.response.
 * Lưu thành 2 phần: description (câu văn tự nhiên) + tags (từ khoá ngắn để search/matching).
 */
public record OllamaTags(
    String description,
    List<String> tags
) {
}
