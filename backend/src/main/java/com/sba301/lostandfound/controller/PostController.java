package com.sba301.lostandfound.controller;

import com.sba301.lostandfound.dto.ApiResponse;
import com.sba301.lostandfound.dto.CreateFoundPostRequest;
import com.sba301.lostandfound.dto.CreateLostPostRequest;
import com.sba301.lostandfound.dto.CreatePostResponse;
import com.sba301.lostandfound.dto.PageResponse;
import com.sba301.lostandfound.dto.PostAdminDTO;
import com.sba301.lostandfound.dto.QuestionSuggestionResponse;
import com.sba301.lostandfound.dto.OllamaQuestionsResponse;
import com.sba301.lostandfound.entity.enums.PostStatus;
import com.sba301.lostandfound.entity.enums.PostType;
import com.sba301.lostandfound.service.PostService;
import com.sba301.lostandfound.service.ImageStorageService;
import com.sba301.lostandfound.service.ImageAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.Optional;
import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final ImageStorageService imageStorageService;
    private final ImageAnalysisService imageAnalysisService;

    @PostMapping(value = "/suggest-questions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<QuestionSuggestionResponse>> suggestQuestions(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "description", required = false) String description) {
        
        if (image == null || image.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Image is required to generate questions");
        }
        
        String imageUrl = imageStorageService.upload(image);
        Optional<OllamaQuestionsResponse> suggestionsOpt = imageAnalysisService.generateQuestions(imageUrl, description);
        
        List<OllamaQuestionsResponse.OllamaQuestion> questions = suggestionsOpt
                .map(OllamaQuestionsResponse::questions)
                .orElse(List.of());
        
        QuestionSuggestionResponse response = new QuestionSuggestionResponse(imageUrl, questions);
        return ResponseEntity.ok(ApiResponse.success(response, "Questions generated successfully"));
    }

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

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<PageResponse<PostAdminDTO>>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) PostType type,
            @RequestParam(required = false) PostStatus status) {
        PageResponse<PostAdminDTO> response = postService.getAllPosts(page, size, sortBy, sortDir, type, status);
        return ResponseEntity.ok(ApiResponse.success(response, "Get all posts successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updatePostStatus(
            @PathVariable Long id,
            @RequestParam PostStatus status) {
        postService.updatePostStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(null, "Update post status successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Delete post successfully"));
    }
}
