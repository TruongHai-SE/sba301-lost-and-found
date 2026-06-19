package com.sba301.lostandfound.service.impl;

import java.net.URI;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Sinh URL ảnh ĐÃ MỜ (blurred) từ URL ảnh gốc trên Cloudinary.
 *
 * <p>Cloudinary hỗ trợ transformation {@code e_blur:N} chèn vào URL để blur ảnh.
 * Ví dụ:
 * <pre>
 *   original: https://res.cloudinary.com/demo/image/upload/v1/sample.jpg
 *   blurred:  https://res.cloudinary.com/demo/image/upload/e_blur:2000/v1/sample.jpg
 * </pre>
 *
 * <p>Ngưỡng blur 2000 (~max) đảm bảo ảnh mờ đủ để che nội dung, người xem chỉ
 * nhận biết "đây là 1 chiếc ví/điện thoại/..." mà không đọc được chi tiết riêng tư.
 *
 * <p>Nếu URL gốc không phải Cloudinary (không match pattern), trả về null
 * để caller quyết định fallback (giữ nguyên ảnh gốc + thêm cảnh báo, hoặc ẩn hoàn toàn).
 */
@Service
public class ImageBlurringService {

    private static final Logger log = LoggerFactory.getLogger(ImageBlurringService.class);

    /** Mức blur (1..2000). 2000 gần như che hết nội dung. */
    private static final int BLUR_STRENGTH = 2000;

    /**
     * Sinh URL blurred từ URL Cloudinary gốc.
     * Trả về null nếu URL không phải Cloudinary.
     */
    public String blur(String originalUrl) {
        if (originalUrl == null || originalUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(originalUrl);
            String host = uri.getHost();
            if (host == null || !host.endsWith(".cloudinary.com")) {
                log.debug("URL is not Cloudinary, cannot blur: {}", originalUrl);
                return null;
            }
            String path = uri.getPath();
            if (path == null || !path.contains("/upload/")) {
                return null;
            }
            // Insert e_blur:N ngay sau "/upload/"
            String blurred = path.replaceFirst(
                "/upload/",
                "/upload/e_blur:" + BLUR_STRENGTH + "/"
            );
            return uri.getScheme() + "://" + host + blurred
                + (uri.getQuery() == null ? "" : "?" + uri.getQuery());
        } catch (URISyntaxException exception) {
            log.warn("Cannot parse URL for blur: {}", originalUrl);
            return null;
        }
    }
}
