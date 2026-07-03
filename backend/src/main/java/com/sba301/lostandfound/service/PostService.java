package com.sba301.lostandfound.service;


import com.sba301.lostandfound.dto.CreateFoundPostRequest;
import com.sba301.lostandfound.dto.CreateLostPostRequest;
import com.sba301.lostandfound.dto.CreatePostResponse;
import com.sba301.lostandfound.dto.FullPostDetails;
import com.sba301.lostandfound.dto.PageResponse;
import com.sba301.lostandfound.dto.PostListResponse;
import com.sba301.lostandfound.entity.enums.PostStatus;
import com.sba301.lostandfound.entity.enums.PostType;

public interface PostService {

    CreatePostResponse createLostPost(CreateLostPostRequest request);

    CreatePostResponse createFoundPost(CreateFoundPostRequest request);

    PageResponse<PostListResponse> getAllPosts(int page, int size, String sortBy, String direction, PostType type, PostStatus status);

    PageResponse<PostListResponse> getUserPosts(int page, int size, String sortBy, String direction, PostType type, PostStatus status, Long userId);

    PageResponse<PostListResponse> filterPosts(com.sba301.lostandfound.dto.PostFilterRequest request, int page, int size, String sortBy, String direction, PostType type, PostStatus status);


    FullPostDetails getPostById(Long id);

    void updatePostStatus(Long id, PostStatus status);

    void deletePost(Long id);

    com.sba301.lostandfound.dto.QuestionSuggestionResponse suggestQuestions(
        org.springframework.web.multipart.MultipartFile image, String description);

    com.sba301.lostandfound.dto.GenerateDescriptionResponse generateDescription(
        org.springframework.web.multipart.MultipartFile image, String description);
}
