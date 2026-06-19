package com.sba301.lostandfound.controller;

import com.sba301.lostandfound.dto.CreateFoundPostRequest;
import com.sba301.lostandfound.dto.CreateLostPostRequest;
import com.sba301.lostandfound.dto.CreatePostResponse;
import com.sba301.lostandfound.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePostResponse createLostPost(@Valid @ModelAttribute CreateLostPostRequest request) {
        return postService.createLostPost(request);
    }

    @PostMapping(value = "/found", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePostResponse createFoundPost(@Valid @ModelAttribute CreateFoundPostRequest request) {
        return postService.createFoundPost(request);
    }
}
