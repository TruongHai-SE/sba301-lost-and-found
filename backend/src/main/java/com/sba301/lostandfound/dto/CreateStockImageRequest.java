package com.sba301.lostandfound.dto;

import com.sba301.lostandfound.entity.enums.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateStockImageRequest {

    @NotNull
    private Category category;

    @NotBlank
    @com.fasterxml.jackson.annotation.JsonProperty("image_url")
    @com.fasterxml.jackson.annotation.JsonAlias("imageUrl")
    private String imageUrl;

    private String label;
}
