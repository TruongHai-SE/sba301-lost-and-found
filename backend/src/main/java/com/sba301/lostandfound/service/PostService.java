package com.sba301.lostandfound.service;

import com.sba301.lostandfound.dto.CreateFoundPostRequest;
import com.sba301.lostandfound.dto.CreateLostPostRequest;
import com.sba301.lostandfound.dto.CreatePostResponse;

import com.sba301.lostandfound.dto.PageResponse;
import com.sba301.lostandfound.dto.PostAdminDTO;
import com.sba301.lostandfound.entity.enums.PostStatus;
import com.sba301.lostandfound.entity.enums.PostType;

public interface PostService {

    CreatePostResponse createLostPost(CreateLostPostRequest request);

    CreatePostResponse createFoundPost(CreateFoundPostRequest request);

    PageResponse<PostAdminDTO> getAllPosts(int page, int size, String sortBy, String direction, PostType type, PostStatus status);

    void updatePostStatus(Long id, PostStatus status);

    void deletePost(Long id);
}
