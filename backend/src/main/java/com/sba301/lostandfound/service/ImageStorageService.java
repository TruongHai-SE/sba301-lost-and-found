package com.sba301.lostandfound.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    /**
     * Upload một file ảnh lên nơi lưu trữ và trả về URL công khai (secure URL).
     */
    String upload(MultipartFile file);

    /**
     * Download ảnh từ một URL công khai rồi re-host lên nơi lưu trữ (Cloudinary),
     * trả về secure URL mới.
     *
     * <p>Dùng khi client chỉ có link ảnh (không có file) và hệ thống muốn tự host
     * ảnh để không phụ thuộc vào nguồn gốc của URL.
     *
     * @param sourceUrl URL ảnh công khai (http/https) cần re-host
     * @return secure URL trên Cloudinary
     */
    String uploadFromUrl(String sourceUrl);
}