package com.sba301.lostandfound.service.impl;

import com.sba301.lostandfound.client.ClipClient;
import com.sba301.lostandfound.dto.*;
import com.sba301.lostandfound.util.StringSanitizer;

import java.util.Base64;
import java.io.IOException;

import com.sba301.lostandfound.entity.VerificationAnswer;
import com.sba301.lostandfound.entity.Image;
import com.sba301.lostandfound.entity.Location;
import com.sba301.lostandfound.entity.Post;
import com.sba301.lostandfound.entity.StockImage;
import com.sba301.lostandfound.entity.User;
import com.sba301.lostandfound.entity.Verification;
import com.sba301.lostandfound.entity.enums.Category;
import com.sba301.lostandfound.entity.enums.HidePostType;
import com.sba301.lostandfound.entity.enums.PostStatus;
import com.sba301.lostandfound.entity.enums.PostType;
import com.sba301.lostandfound.entity.enums.UserType;
import com.sba301.lostandfound.repository.VerificationAnswerRepository;
import com.sba301.lostandfound.repository.ImageRepository;
import com.sba301.lostandfound.repository.LocationRepository;
import com.sba301.lostandfound.repository.PostRepository;
import com.sba301.lostandfound.repository.StockImageRepository;
import com.sba301.lostandfound.repository.UserRepository;
import com.sba301.lostandfound.repository.VerificationRepository;
import com.sba301.lostandfound.service.ImageStorageService;
import com.sba301.lostandfound.service.PostService;
import com.sba301.lostandfound.service.PostAiEnrichmentService;
import com.sba301.lostandfound.service.ImageAnalysisService;
import com.sba301.lostandfound.specification.PostSpecifications;
import org.springframework.web.multipart.MultipartFile;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    private final ImageAnalysisService imageAnalysisService;
    private final ClipClient clipClient;
    private final VerificationRepository verificationRepository;
    private final VerificationAnswerRepository verificationAnswerRepository;
    private final PostAiEnrichmentService postAiEnrichmentService;
    private final ImageBlurringService imageBlurringService;
    private final PostMapper postMapper;
    private final StockImageRepository stockImageRepository;

    public PostServiceImpl(
            UserRepository userRepository,
            LocationRepository locationRepository,
            ImageRepository imageRepository,
            PostRepository postRepository,
            ImageStorageService imageStorageService,
            ImageAnalysisService imageAnalysisService,
            ClipClient clipClient,
            VerificationRepository verificationRepository,
            VerificationAnswerRepository verificationAnswerRepository,
            PostAiEnrichmentService postAiEnrichmentService,
            ImageBlurringService imageBlurringService,
            PostMapper postMapper,
            StockImageRepository stockImageRepository) {
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.imageRepository = imageRepository;
        this.postRepository = postRepository;
        this.imageStorageService = imageStorageService;
        this.imageAnalysisService = imageAnalysisService;
        this.clipClient = clipClient;
        this.verificationRepository = verificationRepository;
        this.verificationAnswerRepository = verificationAnswerRepository;
        this.postAiEnrichmentService = postAiEnrichmentService;
        this.imageBlurringService = imageBlurringService;
        this.postMapper = postMapper;
        this.stockImageRepository = stockImageRepository;
    }

    @Override
    public CreatePostResponse createLostPost(CreateLostPostRequest request) {
        User user = resolveUser(request.getUserId());
        Location location = request.hasLocation() ? saveLocation(request) : null;

        boolean isStockImage = request.getStockImageId() != null;
        Image image;

        if (isStockImage) {
            image = resolveStockImage(request.getStockImageId());
        } else if (request.hasImage()) {
            image = uploadAndSaveImage(request);
        } else {
            image = resolveCategoryStockImage(request.getCategory());
            isStockImage = (image != null);
        }

        HidePostType hidePostType = request.getHidePostType() == null ? HidePostType.PUBLIC : request.getHidePostType();

        // Pre-validation: Skip for stock images (generic placeholder, no need to validate)
        if (!isStockImage && request.hasImage()) {
            ImageValidationResponse validation = clipClient.validateImage(
                request.getImage(),
                request.getImageUrl(),
                request.getTitle()
            );
            if (validation != null && !validation.isValid()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, validation.message());
            }
        }

        Post post = postRepository.save(Post.builder()
                .user(user)
                .location(location)
                .image(image)
                .title(StringSanitizer.sanitizeTitle(request.getTitle()))
                .description(StringSanitizer.sanitizeDescription(request.getDescription()))
                .category(request.getCategory())
                .tags(StringSanitizer.sanitizeTags(request.getTags()))
                .type(PostType.LOST)
                .eventTime(request.getEventTime())
                .createdAt(LocalDateTime.now())
                .status(PostStatus.ACTIVE)
                .hidePostType(hidePostType)
                .isStockImage(isStockImage)
                .build());

        triggerAiEnrichment(post, image, request.getDescription(), null);

        List<ClipMatch> matches = runClipMatching(post, image, request.getDescription());

        return CreatePostResponse.from(post, matches);
    }

    @Override
    public CreatePostResponse createFoundPost(CreateFoundPostRequest request) {
        User user;
        if (request.getUserId() != null) {
            user = resolveUser(request.getUserId());
        } else {
            if (request.getPhone() == null || request.getPhone().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID or Phone number is required");
            }
            user = userRepository.findByPhone(request.getPhone())
                    .orElseGet(() -> userRepository.save(User.builder()
                            .phone(request.getPhone())
                            .name("Guest_" + request.getPhone())
                            .type(UserType.USER)
                            .createdAt(LocalDate.now())
                            .build()));
        }

        boolean isStockImage = request.getStockImageId() != null;

        // 2. Lưu vị trí và hình ảnh
        Location location = request.hasLocation() ? saveLocationForFound(request) : null;

        // Pre-validation: Skip for stock images (generic placeholder, no need to validate)
        if (!isStockImage && request.hasImage()) {
            ImageValidationResponse validation = clipClient.validateImage(
                request.getImage(),
                request.getImageUrl(),
                request.getTitle()
            );
            if (validation != null && !validation.isValid()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, validation.message());
            }
        }

        Image image;
        if (isStockImage) {
            image = resolveStockImage(request.getStockImageId());
        } else if (request.hasImage()) {
            image = uploadAndSaveImageForFound(request);
        } else {
            image = resolveCategoryStockImage(request.getCategory());
            isStockImage = (image != null);
        }

        HidePostType hidePostType = request.getHidePostType() == null ? HidePostType.PUBLIC : request.getHidePostType();

        // 3. Khởi tạo thực thể bài đăng loại FOUND
        Post post = postRepository.save(Post.builder()
                .user(user)
                .location(location)
                .image(image)
                .title(StringSanitizer.sanitizeTitle(request.getTitle()))
                .description(StringSanitizer.sanitizeDescription(request.getDescription()))
                .category(request.getCategory())
                .tags(StringSanitizer.sanitizeTags(request.getTags()))
                .type(PostType.FOUND)
                .eventTime(request.getEventTime())
                .createdAt(LocalDateTime.now())
                .status(PostStatus.ACTIVE)
                .hidePostType(hidePostType)
                .isStockImage(isStockImage)
                .build());

        // 4. Lưu dữ liệu bộ câu hỏi bảo mật động và đáp án gốc
        if (request.getCustomQuestionsJson() != null && !request.getCustomQuestionsJson().isBlank()) {
            postAiEnrichmentService.saveCustomQuestions(post.getId(), request.getCustomQuestionsJson());
        } else if (request.getVerifications() != null) {
            for (VerificationQuestionRequest vReq : request.getVerifications()) {
                Verification verification = verificationRepository.save(Verification.builder()
                        .post(post)
                        .title(vReq.getTitle())
                        .importantPoint(vReq.getImportantPoint() == null ? 5 : vReq.getImportantPoint())
                        .build());

                verificationAnswerRepository.save(VerificationAnswer.builder()
                        .verification(verification)
                        .answer(vReq.getCorrectAnswer())
                        .build());
            }
        }

        triggerAiEnrichment(post, image, request.getDescription(), request.getCustomQuestionsJson());

        // 5. Đồng bộ Vector hóa thông qua CLIP Service và tìm kiếm chéo các bài LOST tương đồng
        List<ClipMatch> matches = runClipMatchingForFound(post, image, request.getDescription());

        return CreatePostResponse.from(post, matches);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostListResponse> getUserPosts(int page, int size, String sortBy, String direction,
                                                       PostType type, PostStatus status, Long userId) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Post> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Post> postPage = postRepository.findAll(spec, pageable);
        Page<PostListResponse> dtoPage = postPage.map(PostListResponse::from);

        return PageResponse.from(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public FullPostDetails getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        return postMapper.toFullDetails(post, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostListResponse> getAllPosts(int page, int size, String sortBy, String direction,
                                                      PostType type, PostStatus status) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Post> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Post> postPage = postRepository.findAll(spec, pageable);
        Page<PostListResponse> dtoPage = postPage.map(PostListResponse::from);

        return PageResponse.from(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostListResponse> filterPosts(PostFilterRequest request, int page,
                                                      int size, String sortBy, String direction, PostType type, PostStatus status) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Post> spec = PostSpecifications.combine(
                PostSpecifications.hasType(type),
                PostSpecifications.hasStatus(status),
                request == null ? null : PostSpecifications.hasCategory(request.getCategory()),
                request == null ? null : PostSpecifications.districtLike(request.getDistrict()),
                request == null ? null : PostSpecifications.eventTimeMatches(request.getDate(), request.getTime()),
                request == null ? null : PostSpecifications.tagLike(request.getTag()));

        Page<Post> postPage = postRepository.findAll(spec, pageable);

        Page<PostListResponse> dtoPage = postPage.map(PostListResponse::from);
        return PageResponse.from(dtoPage);
    }

    @Override
    @Transactional
    public void updatePostStatus(Long id, PostStatus status) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        post.setStatus(status);
        postRepository.save(post);
    }

    @Override
    @Transactional
    public void deletePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        post.setDeleteAt(LocalDateTime.now());
        post.setStatus(PostStatus.DELETED);
        postRepository.save(post);
    }

    // ============================================================================================//

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
        String originalUrl = (request.getImage() != null && !request.getImage().isEmpty())
                ? imageStorageService.upload(request.getImage())
                : request.getImageUrl();
        String blurredUrl = imageBlurringService.blur(originalUrl);
        if (blurredUrl == null) {
            blurredUrl = originalUrl;
        }
        return imageRepository.save(Image.builder()
                .url(blurredUrl)
                .privateUrl(originalUrl)
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * Tạo Image record từ stock image URL. Không upload lên Cloudinary,
     * không blur (vì ảnh mẫu generic, hiển thị rõ luôn).
     */
    private Image resolveStockImage(Long stockImageId) {
        StockImage stockImage = stockImageRepository.findById(stockImageId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Stock image not found: " + stockImageId));
        return imageRepository.save(Image.builder()
                .url(stockImage.getImageUrl())
                .privateUrl(stockImage.getImageUrl())
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * Tự động lấy ảnh mẫu mặc định dựa trên Category khi người dùng không chọn ảnh.
     */
    private Image resolveCategoryStockImage(Category category) {
        if (category == null) {
            category = Category.OTHER;
        }
        List<StockImage> stockImages = stockImageRepository.findByCategory(category);
        if (stockImages.isEmpty()) {
            stockImages = stockImageRepository.findByCategory(Category.OTHER);
        }
        if (stockImages.isEmpty()) {
            return null;
        }
        StockImage stockImage = stockImages.get(0);
        return imageRepository.save(Image.builder()
                .url(stockImage.getImageUrl())
                .privateUrl(stockImage.getImageUrl())
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * Gọi CLIP service để tạo embedding và tìm các tin FOUND có khả năng khớp.
     * Nếu CLIP lỗi/không khả dụng thì vẫn giữ post đã lưu và trả về danh sách rỗng.
     * Stock image posts: chỉ embed text, KHÔNG embed image.
     */
    private List<ClipMatch> runClipMatching(Post post, Image image, String description) {
        try {
            ClipEmbedResponse response;
            String text = buildText(post);
            boolean stockImage = Boolean.TRUE.equals(post.getIsStockImage());

            if (image != null && !stockImage) {
                response = clipClient.embedImage(post.getId(), image.getPrivateUrl(), image.getId(), LOST);
                try {
                    clipClient.embedText(post.getId(), text, LOST);
                } catch (Exception e) {
                    log.warn("Failed to index text embedding for post with image {}: {}", post.getId(), e.getMessage());
                }
            } else {
                response = clipClient.embedText(post.getId(), text, LOST);
            }
            if (response == null || response.matches() == null) {
                return List.of();
            }
            return enrichClipMatches(response.matches());
        } catch (RuntimeException exception) {
            log.warn("CLIP matching failed for post {}: {}", post.getId(), exception.getMessage());
            return List.of();
        }
    }

    private String buildText(Post post) {
        if (post == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (post.getTitle() != null && !post.getTitle().isBlank()) {
            sb.append(post.getTitle().trim());
        }
        if (post.getCategory() != null) {
            String catName = getCategoryDisplayName(post.getCategory());
            if (!catName.isBlank()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(catName);
            }
        }
        if (post.getTags() != null && !post.getTags().isEmpty()) {
            String joinedTags = String.join(", ", post.getTags());
            if (!joinedTags.isBlank()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(joinedTags);
            }
        }
        return sb.toString().trim();
    }

    private String getCategoryDisplayName(Category category) {
        if (category == null) return "";
        return switch (category) {
            case WALLET -> "Ví/Bóp";
            case BAG_BACKPACK -> "Túi/Balo";
            case DOCS_CARDS -> "Giấy tờ/Thẻ";
            case ELECTRONICS -> "Đồ điện tử";
            case KEYS -> "Chìa khóa";
            case APPAREL_ACC -> "Quần áo/Phụ kiện";
            case BOOKS_STATIONERY -> "Sách/Sổ/Bút";
            case PETS -> "Thú cưng";
            case OTHER -> "Khác";
        };
    }

    private List<ClipMatch> enrichClipMatches(List<ClipMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return List.of();
        }
        return matches.stream()
                .map(match -> {
                    String url = null;
                    if (match.imageId() != null) {
                        url = imageRepository.findById(match.imageId())
                                .map(Image::getUrl)
                                .orElse(null);
                    }
                    return ClipMatch.withUrl(match, url);
                })
                .toList();
    }

    private List<ClipMatch> runClipMatchingForFound(Post post, Image image, String description) {
        try {
            ClipEmbedResponse response;
            String text = buildText(post);
            boolean stockImage = Boolean.TRUE.equals(post.getIsStockImage());

            if (image != null && !stockImage) {
                response = clipClient.embedImage(post.getId(), image.getPrivateUrl(), image.getId(), "FOUND");
                try {
                    clipClient.embedText(post.getId(), text, "FOUND");
                } catch (Exception e) {
                    log.warn("Failed to index text embedding for found post with image {}: {}", post.getId(),
                            e.getMessage());
                }
            } else {
                response = clipClient.embedText(post.getId(), text, "FOUND");
            }
            if (response == null || response.matches() == null) {
                return List.of();
            }
            return enrichClipMatches(response.matches());
        } catch (RuntimeException exception) {
            log.warn("CLIP matching failed for found post {}: {}", post.getId(), exception.getMessage());
            return List.of();
        }
    }

    private Location saveLocationForFound(CreateFoundPostRequest request) {
        return locationRepository.save(Location.builder()
                .address(request.getAddress())
                .city(request.getCity())
                .district(request.getDistrict())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationLevel(request.getLocationLevel())
                .build());
    }

    private Image uploadAndSaveImageForFound(CreateFoundPostRequest request) {
        String originalUrl = (request.getImage() != null && !request.getImage().isEmpty())
                ? imageStorageService.upload(request.getImage())
                : request.getImageUrl();
        String blurredUrl = imageBlurringService.blur(originalUrl);
        if (blurredUrl == null) {
            blurredUrl = originalUrl;
        }
        return imageRepository.save(Image.builder()
                .url(blurredUrl)
                .privateUrl(originalUrl)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void triggerAiEnrichment(Post post, Image image, String userDescription, String customQuestionsJson) {
        if (image == null) {
            return;
        }

        // Stock images are generic placeholders — skip AI question generation
        if (Boolean.TRUE.equals(post.getIsStockImage())) {
            return;
        }

        // AI tự sinh câu hỏi + đáp án xác minh (chỉ áp dụng cho FOUND nếu không có
        // custom questions)
        if (post.getType() == PostType.FOUND && (customQuestionsJson == null || customQuestionsJson.isBlank())) {
            try {
                postAiEnrichmentService.generateVerificationQuestionsAsync(
                        post.getId(), image.getPrivateUrl(), userDescription);
            } catch (RuntimeException exception) {
                log.warn("Failed to schedule AI question generation for post {}: {}",
                        post.getId(), exception.getMessage());
            }
        }
    }

    private String resizeAndEncodeBase64(byte[] rawBytes) {
        try {
            if (rawBytes == null || rawBytes.length == 0) {
                return null;
            }

            java.awt.image.BufferedImage originalImage = javax.imageio.ImageIO.read(
                    new java.io.ByteArrayInputStream(rawBytes));
            if (originalImage == null) {
                // ImageIO không đọc được format (HEIC, WebP...) → trả raw base64
                log.warn("ImageIO could not parse image format, using raw base64");
                return Base64.getEncoder().encodeToString(rawBytes);
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            int maxDim = 512;
            int newWidth = originalWidth;
            int newHeight = originalHeight;

            if (originalWidth > maxDim || originalHeight > maxDim) {
                if (originalWidth > originalHeight) {
                    newWidth = maxDim;
                    newHeight = (originalHeight * maxDim) / originalWidth;
                } else {
                    newHeight = maxDim;
                    newWidth = (originalWidth * maxDim) / originalHeight;
                }
            }

            java.awt.image.BufferedImage resizedImage = new java.awt.image.BufferedImage(
                    newWidth, newHeight, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = resizedImage.createGraphics();
            g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            g.dispose();

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(resizedImage, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();

            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception exception) {
            log.warn("Failed to resize image, falling back to raw image: {}", exception.getMessage());
            return Base64.getEncoder().encodeToString(rawBytes);
        }
    }

    @Override
    public QuestionSuggestionResponse suggestQuestions(MultipartFile image, String description) {
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image is required to generate questions");
        }

        // 0. Đọc bytes 1 lần duy nhất để tránh lỗi stream bị đóng trên server
        byte[] rawBytes;
        try {
            rawBytes = image.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read image bytes");
        }

        // 1. Upload to Cloudinary to get final imageUrl
        String imageUrl = imageStorageService.upload(image);

        // 2. Convert and resize image directly to base64
        String base64Image = resizeAndEncodeBase64(rawBytes);
        if (base64Image == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read image bytes");
        }

        // 3. Generate questions using base64 directly
        Optional<OllamaQuestionsResponse> suggestionsOpt = imageAnalysisService.generateQuestions(base64Image,
                description);

        List<OllamaQuestionsResponse.OllamaQuestion> questions = suggestionsOpt
                .map(OllamaQuestionsResponse::questions)
                .orElse(List.of());

        return new QuestionSuggestionResponse(imageUrl, questions);
    }

    @Override
    public GenerateDescriptionResponse generateDescription(MultipartFile image, String description) {
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image is required to generate description");
        }

        // 0. Đọc bytes 1 lần duy nhất để tránh lỗi stream bị đóng trên server
        byte[] rawBytes;
        try {
            rawBytes = image.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read image bytes");
        }

        // 1. Upload to Cloudinary to get final imageUrl
        String imageUrl = imageStorageService.upload(image);

        // 2. Convert and resize image directly to base64
        String base64Image = resizeAndEncodeBase64(rawBytes);
        if (base64Image == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read image bytes");
        }

        // 3. Analyze image using base64 directly
        Optional<OllamaTags> tagsOpt = imageAnalysisService.analyzeImage(base64Image, description);

        if (tagsOpt.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI could not generate a description from the image. Please try again or write it manually.");
        }

        OllamaTags tags = tagsOpt.get();
        return new GenerateDescriptionResponse(imageUrl, tags.description(), tags.tags());
    }
}
