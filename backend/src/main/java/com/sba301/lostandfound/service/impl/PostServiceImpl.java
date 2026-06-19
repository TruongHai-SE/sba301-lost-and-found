package com.sba301.lostandfound.service.impl;

import com.sba301.lostandfound.client.ClipClient;
import com.sba301.lostandfound.dto.ClipEmbedResponse;
import com.sba301.lostandfound.dto.ClipMatch;
import com.sba301.lostandfound.dto.CreateFoundPostRequest;
import com.sba301.lostandfound.dto.CreateLostPostRequest;
import com.sba301.lostandfound.dto.CreatePostResponse;
import com.sba301.lostandfound.entity.Image;
import com.sba301.lostandfound.entity.Location;
import com.sba301.lostandfound.entity.Post;
import com.sba301.lostandfound.entity.User;
import com.sba301.lostandfound.entity.enums.HidePostType;
import com.sba301.lostandfound.entity.enums.PostStatus;
import com.sba301.lostandfound.entity.enums.PostType;
import com.sba301.lostandfound.repository.ImageRepository;
import com.sba301.lostandfound.repository.LocationRepository;
import com.sba301.lostandfound.repository.PostRepository;
import com.sba301.lostandfound.repository.UserRepository;
import com.sba301.lostandfound.service.ImageStorageService;
import com.sba301.lostandfound.service.PostAiEnrichmentService;
import com.sba301.lostandfound.service.PostService;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PostServiceImpl implements PostService {

    private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);

    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final ImageRepository imageRepository;
    private final PostRepository postRepository;
    private final ImageStorageService imageStorageService;
    private final ClipClient clipClient;
    private final PostAiEnrichmentService postAiEnrichmentService;

    public PostServiceImpl(
        UserRepository userRepository,
        LocationRepository locationRepository,
        ImageRepository imageRepository,
        PostRepository postRepository,
        ImageStorageService imageStorageService,
        ClipClient clipClient,
        PostAiEnrichmentService postAiEnrichmentService
    ) {
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.imageRepository = imageRepository;
        this.postRepository = postRepository;
        this.imageStorageService = imageStorageService;
        this.clipClient = clipClient;
        this.postAiEnrichmentService = postAiEnrichmentService;
    }

    @Override
    public CreatePostResponse createLostPost(CreateLostPostRequest request) {
        User user = resolveUser(request.getUserId());
        Location location = request.hasLocation() ? saveLocation(
            request.getAddress(), request.getCity(), request.getDistrict(),
            request.getLatitude(), request.getLongitude(), request.getLocationLevel()
        ) : null;
        Image image = request.hasImage() ? uploadAndSaveImage(request.getImage()) : null;

        HidePostType hidePostType =
            request.getHidePostType() == null ? HidePostType.PUBLIC : request.getHidePostType();

        Post post = postRepository.save(Post.builder()
            .user(user)
            .location(location)
            .image(image)
            .title(request.getTitle())
            .description(request.getDescription())
            .type(PostType.LOST)
            .eventTime(request.getEventTime())
            .createAt(LocalDateTime.now())
            .status(PostStatus.ACTIVE)
            .hidePostType(hidePostType)
            .build());

        triggerAiEnrichment(post, image, request.getDescription());

        List<ClipMatch> matches = runClipMatching(post, image, request.getDescription());

        return CreatePostResponse.from(post, matches);
    }

    @Override
    public CreatePostResponse createFoundPost(CreateFoundPostRequest request) {
        User user = resolveUser(request.getUserId());
        Location location = request.hasLocation() ? saveLocation(
            request.getAddress(), request.getCity(), request.getDistrict(),
            request.getLatitude(), request.getLongitude(), request.getLocationLevel()
        ) : null;
        Image image = request.hasImage() ? uploadAndSaveImage(request.getImage()) : null;

        HidePostType hidePostType =
            request.getHidePostType() == null ? HidePostType.PUBLIC : request.getHidePostType();

        Post post = postRepository.save(Post.builder()
            .user(user)
            .location(location)
            .image(image)
            .title(request.getTitle())
            .description(request.getDescription())
            .type(PostType.FOUND)
            .eventTime(request.getEventTime())
            .createAt(LocalDateTime.now())
            .status(PostStatus.ACTIVE)
            .hidePostType(hidePostType)
            .build());

        triggerAiEnrichment(post, image, request.getDescription());

        List<ClipMatch> matches = runClipMatching(post, image, request.getDescription());

        return CreatePostResponse.from(post, matches);
    }

    private User resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "User not found: " + userId));
    }

    private Location saveLocation(
        String address, String city, String district,
        Double latitude, Double longitude, Integer locationLevel
    ) {
        return locationRepository.save(Location.builder()
            .address(address)
            .city(city)
            .district(district)
            .latitude(latitude)
            .longitude(longitude)
            .locationLevel(locationLevel)
            .build());
    }

    private Image uploadAndSaveImage(MultipartFile file) {
        String url = imageStorageService.upload(file);
        return imageRepository.save(Image.builder()
            .url(url)
            .createAt(LocalDateTime.now())
            .build());
    }

    /**
     * Kích hoạt các tác vụ AI phân tích ảnh chạy nền.
     * 1. Sinh mô tả chi tiết & tags bằng AI (cho cả LOST và FOUND).
     * 2. Sinh câu hỏi xác minh tự động (chỉ cho FOUND).
     */
    private void triggerAiEnrichment(Post post, Image image, String userDescription) {
        if (image == null) {
            return;
        }

        // Tác vụ 1: AI mô tả chi tiết hình ảnh & gán tags tìm kiếm
        try {
            postAiEnrichmentService.enrichDescriptionAsync(
                post.getId(), image.getUrl(), userDescription
            );
        } catch (RuntimeException exception) {
            log.warn("Failed to schedule AI description enrichment for post {}: {}",
                post.getId(), exception.getMessage());
        }

        // Tác vụ 2: AI tự sinh câu hỏi + đáp án xác minh (chỉ áp dụng cho FOUND)
        if (post.getType() == PostType.FOUND) {
            try {
                postAiEnrichmentService.generateVerificationQuestionsAsync(
                    post.getId(), image.getUrl(), userDescription
                );
            } catch (RuntimeException exception) {
                log.warn("Failed to schedule AI question generation for post {}: {}",
                    post.getId(), exception.getMessage());
            }
        }
    }

    /**
     * Gọi CLIP service để tạo embedding và tìm các tin có khả năng khớp.
     * Loại tin (LOST/FOUND) được truyền động để CLIP tìm kiếm chéo đúng chiều.
     * Nếu CLIP lỗi/không khả dụng thì vẫn giữ post đã lưu và trả về danh sách rỗng.
     */
    private List<ClipMatch> runClipMatching(Post post, Image image, String description) {
        try {
            String postType = post.getType().name();
            ClipEmbedResponse response;
            if (image != null) {
                response = clipClient.embedImage(post.getId(), image.getUrl(), image.getId(), postType);
            } else {
                String text = buildText(post.getTitle(), description);
                response = clipClient.embedText(post.getId(), text, postType);
            }
            if (response == null || response.matches() == null) {
                return List.of();
            }
            return response.matches();
        } catch (RuntimeException exception) {
            log.warn("CLIP matching failed for post {}: {}", post.getId(), exception.getMessage());
            return List.of();
        }
    }

    private String buildText(String title, String description) {
        if (description == null || description.isBlank()) {
            return title;
        }
        return title + ". " + description;
    }
}
