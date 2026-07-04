package com.sba301.lostandfound.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostFilterRequest {

    private LocalDate date;

    // Chặn luồng set dữ liệu từ Spring Boot và tự xử lý
    public void setDate(String dateStr) {
        if (dateStr != null && !dateStr.isEmpty()) {
            // Nếu chuỗi chứa chữ T, cắt lấy phần trước chữ T
            if (dateStr.contains("T")) {
                dateStr = dateStr.split("T")[0];
            }
            this.date = LocalDate.parse(dateStr);
        }
    }


    
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime time;
    
    private String district;
}
