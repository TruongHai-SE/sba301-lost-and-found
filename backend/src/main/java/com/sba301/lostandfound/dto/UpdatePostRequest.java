package com.sba301.lostandfound.dto;

import com.sba301.lostandfound.entity.enums.HidePostType;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class UpdatePostRequest {

    private String title;

    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime eventTime;

    private HidePostType hidePostType;

    private String address;

    private String city;

    private String district;

    private Double latitude;

    private Double longitude;

    private Integer locationLevel;

    public boolean hasLocation() {
        return address != null || city != null || district != null
            || latitude != null || longitude != null || locationLevel != null;
    }
}
