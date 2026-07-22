package com.sba301.lostandfound.dto;

import com.sba301.lostandfound.entity.enums.Category;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStockImageRequest {

    private Category category;

    @com.fasterxml.jackson.annotation.JsonProperty("image_url")
    @com.fasterxml.jackson.annotation.JsonAlias("imageUrl")
    private String imageUrl;

    private String label;
}
