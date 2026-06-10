package com.sba301.lostandfound.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    /**
     * Upload một file ảnh lên nơi lưu trữ và trả về URL công khai (secure URL).
     */
    String upload(MultipartFile file);
}
