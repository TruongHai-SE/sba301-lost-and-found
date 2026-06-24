package com.sba301.lostandfound.dto;

import java.util.List;

/**
 * Kết quả khi người dùng CHỦ ĐỘNG bấm nút "Sinh mô tả từ ảnh" lúc đăng tin.
 *
 * Khác với flow enrich nền (chạy ngầm sau khi tạo post), flow này:
 *  - Đồng bộ: bấm nút là có kết quả ngay để điền vào ô mô tả.
 *  - Không tạo/sửa post nào. Frontend tự quyết định dùng hay không.
 *
 * Trả kèm {@code imageUrl} (ảnh đã upload) để khi submit form, frontend dùng lại
 * URL này thay vì upload ảnh lần nữa.
 */
public record GenerateDescriptionResponse(
    String imageUrl,
    String description,
    List<String> tags
) {}
