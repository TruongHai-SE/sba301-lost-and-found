package com.sba301.lostandfound.controller;

import com.sba301.lostandfound.dto.ApiResponse;
import com.sba301.lostandfound.dto.CreateFoundPostRequest;
import com.sba301.lostandfound.dto.CreateLostPostRequest;
import com.sba301.lostandfound.dto.CreatePostResponse;
import com.sba301.lostandfound.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CreatePostResponse>> createLostPost(
            @Valid @ModelAttribute CreateLostPostRequest request) {
        CreatePostResponse response = postService.createLostPost(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), response, "Create post successfully"));
    }

    @PostMapping(value = "/found", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CreatePostResponse>> createFoundPost(
            @Valid @ModelAttribute CreateFoundPostRequest request) {
        CreatePostResponse response = postService.createFoundPost(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), response, "Create found post successfully"));
    }
}
