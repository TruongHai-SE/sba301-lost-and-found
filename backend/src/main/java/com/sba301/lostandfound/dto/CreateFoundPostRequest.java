package com.sba301.lostandfound.dto;

import com.sba301.lostandfound.entity.enums.HidePostType;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * Body của API tạo tin nhặt được đồ. Gửi dưới dạng multipart/form-data nên dùng class
 * có getter/setter để Spring bind qua @ModelAttribute. Các field location và image
 * đều tùy chọn.
 */
@Getter
@Setter
public class CreateFoundPostRequest {

    @NotBlank
    private String title;

    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime eventTime;

    private Long userId;

    private HidePostType hidePostType;

    private String address;

    private String city;

    private String district;

    private Double latitude;

    private String phone;
    
    private Double longitude;

    private Integer locationLevel;

    private MultipartFile image;

    private String customQuestionsJson;

    private String imageUrl;

    private List<VerificationQuestionRequest> verifications;

    public boolean hasLocation() {
        return address != null || city != null || district != null
            || latitude != null || longitude != null || locationLevel != null;
    }

    public boolean hasImage() {
        return (image != null && !image.isEmpty()) || (imageUrl != null && !imageUrl.isBlank());
    }
}
