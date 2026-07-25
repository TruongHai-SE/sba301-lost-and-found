package com.sba301.lostandfound.controller;

import com.sba301.lostandfound.dto.ApiResponse;
import com.sba301.lostandfound.dto.CreateFoundPostRequest;
import com.sba301.lostandfound.dto.CreateLostPostRequest;
import com.sba301.lostandfound.dto.CreatePostResponse;
import com.sba301.lostandfound.dto.FullPostDetails;
import com.sba301.lostandfound.dto.PageResponse;
import com.sba301.lostandfound.dto.PostListResponse;
import com.sba301.lostandfound.dto.QuestionSuggestionResponse;
import com.sba301.lostandfound.dto.GenerateDescriptionResponse;
import com.sba301.lostandfound.dto.SearchResponse;
import com.sba301.lostandfound.dto.SearchByTextRequest;
import com.sba301.lostandfound.entity.enums.PostStatus;
import com.sba301.lostandfound.entity.enums.PostType;
import com.sba301.lostandfound.security.CustomUserDetails;
import com.sba301.lostandfound.service.PostService;
import com.sba301.lostandfound.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import com.sba301.lostandfound.dto.PostFilterRequest;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final SearchService searchService;

    @PostMapping(value = "/suggest-questions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<QuestionSuggestionResponse>> suggestQuestions(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "description", required = false) String description) {
        QuestionSuggestionResponse response = postService.suggestQuestions(image, description);
        return ResponseEntity.ok(ApiResponse.success(response, "Questions generated successfully"));
    }

    @PostMapping(value = "/generate-description", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<GenerateDescriptionResponse>> generateDescription(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "description", required = false) String description) {
        GenerateDescriptionResponse response = postService.generateDescription(image, description);
        return ResponseEntity.ok(ApiResponse.success(response, "Description generated successfully"));
    }

    /**
     * Search bằng ảnh (multipart/form-data) kết hợp CLIP embedding.
     * Body:
     * - image: file ảnh (bắt buộc)
     * - description: text mô tả bổ sung (optional)
     * - top_k: số kết quả tối đa (optional, default 10)
     * - target_type: LOST | FOUND | ALL (optional, default FOUND)
     */
    @PostMapping(value = "/search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SearchResponse>> searchByImage(
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "top_k", required = false) Integer topK,
            @RequestParam(value = "target_type", required = false) String targetType) {
        SearchResponse result = searchService.searchByImage(image, description, topK, targetType);
        return ResponseEntity.ok(ApiResponse.success(result, "Search completed successfully"));
    }

    @PostMapping(value = "/search/text", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<SearchResponse>> searchByText(@RequestBody SearchByTextRequest request) {
        SearchResponse result = searchService.searchByText(request);
        return ResponseEntity.ok(ApiResponse.success(result, "Search completed successfully"));
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

    @GetMapping("/my-posts")
    public ResponseEntity<ApiResponse<PageResponse<PostListResponse>>> getMyPosts(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) PostType type,
            @RequestParam(required = false) PostStatus status) {
        Long userId = currentUser.getUser().getId();
        PageResponse<PostListResponse> response = postService.getUserPosts(page, size, sortBy, sortDir, type, status,
                userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Get my posts successfully"));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<PostListResponse>>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) PostType type,
            @RequestParam(required = false) PostStatus status) {
        PageResponse<PostListResponse> response = postService.getUserPosts(page, size, sortBy, sortDir, type, status,
                userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Get user posts successfully"));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<PageResponse<PostListResponse>>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) PostType type,
            @RequestParam(required = false) PostStatus status) {
        PageResponse<PostListResponse> response = postService.getAllPosts(page, size, sortBy, sortDir, type, status);
        return ResponseEntity.ok(ApiResponse.success(response, "Get all posts successfully"));
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<PageResponse<PostListResponse>>> filterPosts(
            @ParameterObject @ModelAttribute PostFilterRequest filterRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) PostType type,
            @RequestParam(required = false) PostStatus status) {
        PageResponse<PostListResponse> response = postService.filterPosts(filterRequest, page, size, sortBy, sortDir,
                type, status);
        return ResponseEntity.ok(ApiResponse.success(response, "Filter posts successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FullPostDetails>> getPostById(
            @PathVariable Long id) {
        FullPostDetails response = postService.getPostById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Get post details successfully"));
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
