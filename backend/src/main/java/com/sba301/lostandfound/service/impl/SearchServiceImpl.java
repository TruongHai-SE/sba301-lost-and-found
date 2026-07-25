package com.sba301.lostandfound.service.impl;

import com.sba301.lostandfound.client.ClipClient;
import com.sba301.lostandfound.dto.BlurredPostSummary;
import com.sba301.lostandfound.dto.ClipEmbedResponse;
import com.sba301.lostandfound.dto.ClipMatch;
import com.sba301.lostandfound.dto.SearchByTextRequest;
import com.sba301.lostandfound.dto.SearchResponse;
import com.sba301.lostandfound.entity.Post;
import com.sba301.lostandfound.entity.enums.PostStatus;
import com.sba301.lostandfound.repository.PostRepository;
import com.sba301.lostandfound.service.ImageStorageService;
import com.sba301.lostandfound.service.SearchService;
import com.sba301.lostandfound.repository.specification.PostSpecifications;
import com.sba301.lostandfound.util.StringSanitizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
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
    /** Cap mặc định để tránh CLIP/DB overload. */
    private static final int MAX_TOP_K = 50;
    /** Cap khi có filter: mở rộng nguồn để tránh kết quả rỗng sau khi filter. */
    private static final int MAX_TOP_K_WITH_FILTER = 100;
    /** Hệ số nhân top_k khi có filter (để CLIP trả nhiều hơn, bù cho phần bị filter loại). */
    private static final int FILTER_TOP_K_MULTIPLIER = 3;
    /** Score tối thiểu để chấp nhận match CLIP. */
    private static final double MIN_MATCH_SCORE = 0.5;

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
        int k = clampTopK(topK);
        String type = normalizeTargetType(targetType);

        ClipEmbedResponse response;
        try {
            response = clipClient.searchByImageBytes(image, type, k);
        } catch (RuntimeException exception) {
            log.warn("CLIP searchByImageBytes failed: {}", exception.getMessage());
            return new SearchResponse("IMAGE", 0, List.of());
        }
        return buildResponseFromClip(response, "IMAGE", type, null);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResponse searchByText(SearchByTextRequest request) {
        if (request == null || request.text() == null || request.text().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Text is required for text search");
        }
        String sanitizedText = StringSanitizer.sanitizeSearchText(request.text());
        if (sanitizedText.isBlank()) {
            return new SearchResponse("TEXT", 0, List.of());
        }
        boolean hasFilter = hasAnyFilter(request);
        int k = clampTopK(request.topK(), hasFilter);
        String type = normalizeTargetType(request.targetType());

        ClipEmbedResponse response;
        try {
            response = clipClient.search(null, sanitizedText, type, k);
        } catch (RuntimeException exception) {
            log.warn("CLIP text search failed: {}", exception.getMessage());
            return new SearchResponse("TEXT", 0, List.of());
        }
        return buildResponseFromClip(response, "TEXT", type, hasFilter ? request : null);
    }

    /**
     * Biến đổi kết quả CLIP (danh sách post_id + score) thành SearchResponse,
     * áp filter (nếu có) lên DB trước khi map sang BlurredPostSummary.
     *
     * @param response      Kết quả từ CLIP (matches theo thứ tự score DESC).
     * @param queryType     IMAGE | TEXT - để echo lại cho FE.
     * @param type          targetType đã normalize (log).
     * @param filterRequest Filter từ search text request (null nếu không có filter).
     */
    private SearchResponse buildResponseFromClip(
            ClipEmbedResponse response, String queryType, String type, SearchByTextRequest filterRequest) {
        if (response == null || response.matches() == null || response.matches().isEmpty()) {
            return new SearchResponse(queryType, 0, List.of());
        }

        List<Long> postIds = response.matches().stream()
                .map(ClipMatch::postId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Post> posts;
        if (filterRequest == null) {
            posts = postIds.isEmpty() ? List.of() : postRepository.findAllByIdIn(postIds);
        } else {
            posts = postIds.isEmpty() ? List.of()
                    : postRepository.findAll(buildFilterSpec(postIds, filterRequest));
        }

        Map<Long, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));

        List<BlurredPostSummary> results = new ArrayList<>();
        for (ClipMatch match : response.matches()) {
            if (match.postId() == null)
                continue;
            if (match.score() != null && match.score() < MIN_MATCH_SCORE)
                continue;
            Post post = postMap.get(match.postId());
            if (post == null)
                continue;
            results.add(postMapper.toBlurredSummary(post, match));
        }

        log.info("Search [{}] target={} returned {} results", queryType, type, results.size());
        return new SearchResponse(queryType, results.size(), results);
    }

    /**
     * Build Specification lọc post theo id IN postIds, đồng thời áp các filter
     * khác (category, district, date/time, tag, status). Mặc định status = ACTIVE
     * để không lộ post DELETED/RESOLVED.
     */
    @SuppressWarnings("unchecked")
    private static Specification<Post> buildFilterSpec(List<Long> postIds, SearchByTextRequest req) {
        PostStatus status = req.status() != null ? req.status() : PostStatus.ACTIVE;
        return PostSpecifications.combine(
                PostSpecifications.idIn(postIds),
                PostSpecifications.hasStatus(status),
                PostSpecifications.hasCategory(req.category()),
                PostSpecifications.districtLike(req.district()),
                PostSpecifications.eventTimeMatches(req.date(), req.time()),
                PostSpecifications.tagLike(req.tag()));
    }

    /** Kiểm tra request có truyền filter nào không (để quyết định mở rộng top_k). */
    private static boolean hasAnyFilter(SearchByTextRequest req) {
        return req.category() != null
                || (req.district() != null && !req.district().isBlank())
                || req.date() != null
                || req.time() != null
                || (req.tag() != null && !req.tag().isBlank())
                || req.status() != null;
    }

    private int clampTopK(Integer topK) {
        return clampTopK(topK, false);
    }

    /**
     * Tính top_k gửi CLIP. Khi có filter, nhân lên để đảm bảo đủ nguồn cho filter
     * (vì filter thu hẹp kết quả), capped theo {@link #MAX_TOP_K_WITH_FILTER}.
     */
    private int clampTopK(Integer topK, boolean hasFilter) {
        int base = (topK == null || topK <= 0) ? DEFAULT_TOP_K : topK;
        if (!hasFilter) {
            return Math.min(base, MAX_TOP_K);
        }
        int expanded = base * FILTER_TOP_K_MULTIPLIER;
        return Math.min(expanded, MAX_TOP_K_WITH_FILTER);
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
