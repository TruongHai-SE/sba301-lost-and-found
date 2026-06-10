package com.sba301.lostandfound.service.impl;

import com.sba301.lostandfound.client.ClipClient;
import com.sba301.lostandfound.dto.ClipEmbedResponse;
import com.sba301.lostandfound.dto.ClipMatch;
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
import com.sba301.lostandfound.service.PostService;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PostServiceImpl implements PostService {

    private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);
    private static final String LOST = PostType.LOST.name();

    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final ImageRepository imageRepository;
    private final PostRepository postRepository;
    private final ImageStorageService imageStorageService;
    private final ClipClient clipClient;

    public PostServiceImpl(
        UserRepository userRepository,
        LocationRepository locationRepository,
        ImageRepository imageRepository,
        PostRepository postRepository,
        ImageStorageService imageStorageService,
        ClipClient clipClient
    ) {
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.imageRepository = imageRepository;
        this.postRepository = postRepository;
        this.imageStorageService = imageStorageService;
        this.clipClient = clipClient;
    }

    @Override
    @Transactional
    public CreatePostResponse createLostPost(CreateLostPostRequest request) {
        User user = resolveUser(request.getUserId());
        Location location = request.hasLocation() ? saveLocation(request) : null;
        Image image = request.hasImage() ? uploadAndSaveImage(request) : null;

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

    private Location saveLocation(CreateLostPostRequest request) {
        return locationRepository.save(Location.builder()
            .address(request.getAddress())
            .city(request.getCity())
            .district(request.getDistrict())
            .latitude(request.getLatitude())
            .longitude(request.getLongitude())
            .locationLevel(request.getLocationLevel())
            .build());
    }

    private Image uploadAndSaveImage(CreateLostPostRequest request) {
        String url = imageStorageService.upload(request.getImage());
        return imageRepository.save(Image.builder()
            .url(url)
            .createAt(LocalDateTime.now())
            .build());
    }

    /**
     * Gọi CLIP service để tạo embedding và tìm các tin FOUND có khả năng khớp.
     * Nếu CLIP lỗi/không khả dụng thì vẫn giữ post đã lưu và trả về danh sách rỗng.
     */
    private List<ClipMatch> runClipMatching(Post post, Image image, String description) {
        try {
            ClipEmbedResponse response;
            if (image != null) {
                response = clipClient.embedImage(post.getId(), image.getUrl(), image.getId(), LOST);
            } else {
                String text = buildText(post.getTitle(), description);
                response = clipClient.embedText(post.getId(), text, LOST);
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
