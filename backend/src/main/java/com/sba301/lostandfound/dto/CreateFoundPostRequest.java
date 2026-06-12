package com.sba301.lostandfound.dto;

import com.sba301.lostandfound.entity.enums.HidePostType;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class CreateFoundPostRequest {
    @NotBlank
    private String title;

    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime eventTime;

    @NotBlank
    private String phone; // Bắt buộc đối với người nhặt được đồ

    private HidePostType hidePostType; // PUBLIC hoặc WHEN_MATCH

    private String address;
    private String city;
    private String district;
    private Double latitude;
    private Double longitude;
    private Integer locationLevel;

    private MultipartFile image;

    // Danh sách câu hỏi xác minh và đáp án tương ứng
    private List<VerificationQuestionRequest> verifications;

    public boolean hasLocation() {
        return address != null || city != null || district != null
            || latitude != null || longitude != null || locationLevel != null;
    }

    public boolean hasImage() {
        return image != null && !image.isEmpty();
    }
}