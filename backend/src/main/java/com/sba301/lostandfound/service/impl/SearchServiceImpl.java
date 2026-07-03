package com.sba301.lostandfound.service.impl;

import com.sba301.lostandfound.client.ClipClient;
import com.sba301.lostandfound.dto.BlurredPostSummary;
import com.sba301.lostandfound.dto.ClipEmbedResponse;
import com.sba301.lostandfound.dto.ClipMatch;
import com.sba301.lostandfound.dto.SearchResponse;
import com.sba301.lostandfound.entity.Post;
import com.sba301.lostandfound.repository.PostRepository;
import com.sba301.lostandfound.service.ImageStorageService;
import com.sba301.lostandfound.service.SearchService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implementation: search dùng CLIP làm "retriever", sau đó join với PostgreSQL
 * để lấy full thông tin post (kèm câu hỏi xác minh).
 *
 * <p>
 * Flow:
 * 1. Upload ảnh query lên Cloudinary (tạo URL public).
 * 2. Gọi CLIP service /api/v1/search với query_image_url + target_post_type.
 * 3. CLIP trả về danh sách post_id + score (cross-type matching).
 * 4. Query DB lấy Post entity theo post_id.
 * 5. Map sang BlurredPostSummary (ảnh mờ + câu hỏi xác minh).
 */
@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);

    /** Default topK nếu FE không truyền. */
    private static final int DEFAULT_TOP_K = 10;
    /** Cap để tránh CLIP/DB overload. */
    private static final int MAX_TOP_K = 50;

    private final ImageStorageService imageStorageService;
    private final ClipClient clipClient;
    private final PostRepository postRepository;
    private final PostMapper postMapper;

    public SearchServiceImpl(
            ImageStorageService imageStorageService,
            ClipClient clipClient,
            PostRepository postRepository,
            PostMapper postMapper) {
        this.imageStorageService = imageStorageService;
        this.clipClient = clipClient;
        this.postRepository = postRepository;
        this.postMapper = postMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResponse searchByImage(
            MultipartFile image,
            String description,
            Integer topK,
            String targetType) {
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Image is required for image search");
        }
        String imageUrl = imageStorageService.upload(image);
        return doSearch(imageUrl, description, topK, targetType, "IMAGE");
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResponse searchByText(String text, Integer topK, String targetType) {
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Text is required for text search");
        }
        return doSearch(null, text, topK, targetType, "TEXT");
    }

    /**
     * Gọi CLIP search, sau đó enrich kết quả với thông tin từ DB.
     *
     * @param queryImageUrl URL ảnh (cho image search) hoặc null (cho text search)
     * @param queryText     Text mô tả (bắt buộc nếu image null)
     * @param topK          Số kết quả tối đa
     * @param targetType    LOST | FOUND | ALL
     * @param queryType     IMAGE | TEXT - để echo lại cho FE
     */
    private SearchResponse doSearch(
            String queryImageUrl, String queryText, Integer topK, String targetType, String queryType) {
        int k = clampTopK(topK);
        String type = normalizeTargetType(targetType);

        ClipEmbedResponse response;
        try {
            response = clipClient.search(queryImageUrl, queryText, type, k);
        } catch (RuntimeException exception) {
            log.warn("CLIP search failed: {}", exception.getMessage());
            return new SearchResponse(queryType, 0, List.of());
        }

        if (response == null || response.matches() == null || response.matches().isEmpty()) {
            return new SearchResponse(queryType, 0, List.of());
        }

        List<Long> postIds = response.matches().stream()
                .map(ClipMatch::postId)
                .filter(Objects::nonNull)
                .toList();

        List<Post> posts = postIds.isEmpty() ? List.of() : postRepository.findAllByIdIn(postIds);

        Map<Long, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));

        List<BlurredPostSummary> results = new ArrayList<>();
        for (ClipMatch match : response.matches()) {
            if (match.postId() == null || (match.score() != null && match.score() < 0.5))
                continue;
            Post post = postMap.get(match.postId());
            if (post == null)
                continue;
            results.add(postMapper.toBlurredSummary(post, match));
        }

        log.info("Search [{}] target={} returned {} results", queryType, type, results.size());
        return new SearchResponse(queryType, results.size(), results);
    }

    private int clampTopK(Integer topK) {
        if (topK == null || topK <= 0)
            return DEFAULT_TOP_K;
        return Math.min(topK, MAX_TOP_K);
    }

    /**
     * Normalize target type. Mặc định = "FOUND" (vì use case chính là người mất
     * tìm).
     */
    private String normalizeTargetType(String raw) {
        if (raw == null || raw.isBlank())
            return "FOUND";
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "LOST", "FOUND", "ALL" -> upper;
            default -> "FOUND";
        };
    }
}
