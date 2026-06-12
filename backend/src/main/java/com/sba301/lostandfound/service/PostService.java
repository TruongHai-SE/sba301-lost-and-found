package com.sba301.lostandfound.service;

import com.sba301.lostandfound.dto.CreateFoundPostRequest;
import com.sba301.lostandfound.dto.CreateLostPostRequest;
import com.sba301.lostandfound.dto.CreatePostResponse;

public interface PostService {

    CreatePostResponse createLostPost(CreateLostPostRequest request);

    CreatePostResponse createFoundPost(CreateFoundPostRequest request);
}
