package com.sba301.lostandfound.dto;

import com.sba301.lostandfound.entity.enums.HidePostType;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

/**
 * Body của API tạo tin mất đồ. Gửi dưới dạng multipart/form-data nên dùng class
 * có getter/setter để Spring bind qua @ModelAttribute. Các field location và image
 * đều tùy chọn.
 */
import com.sba301.lostandfound.entity.enums.Category;
import java.util.List;

@Getter
@Setter
public class CreateLostPostRequest {

    @NotBlank
    private String title;

    private String description;

    @jakarta.validation.constraints.NotNull(message = "Category is required")
    private Category category;

    private List<String> tags;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime eventTime;

    private Long userId;

    private HidePostType hidePostType;

    private String address;

    private String city;

    private String district;

    private Double latitude;

    private Double longitude;

    private Integer locationLevel;

    private MultipartFile image;

    /**
     * URL ảnh đã upload trước đó (vd: khi người dùng bấm "Sinh mô tả từ ảnh").
     * Dùng để tái sử dụng ảnh, tránh upload lần hai. Bỏ qua nếu {@link #image} có file.
     */
    private String imageUrl;

    /**
     * ID của ảnh mẫu (stock image) từ bảng stock_images.
     * Nếu có giá trị, sẽ dùng URL ảnh mẫu thay vì upload ảnh thật.
     */
    private Long stockImageId;

    public boolean hasLocation() {
        return address != null || city != null || district != null
            || latitude != null || longitude != null || locationLevel != null;
    }

    public boolean hasImage() {
        return (image != null && !image.isEmpty())
            || (imageUrl != null && !imageUrl.isBlank())
            || stockImageId != null;
    }
}
