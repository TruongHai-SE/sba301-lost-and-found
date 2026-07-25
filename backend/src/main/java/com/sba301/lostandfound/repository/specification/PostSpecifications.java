package com.sba301.lostandfound.repository.specification;

import com.sba301.lostandfound.entity.Post;
import com.sba301.lostandfound.entity.enums.Category;
import com.sba301.lostandfound.entity.enums.PostStatus;
import com.sba301.lostandfound.entity.enums.PostType;
import com.sba301.lostandfound.util.StringSanitizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Các builder Specification<Post> dùng chung cho filter và search,
 * tránh trùng lặp logic predicate giữa PostServiceImpl (filter) và
 * SearchServiceImpl (text search + filter).
 */
public final class PostSpecifications {

    private PostSpecifications() {
    }

    /** Lọc post theo id nằm trong danh sách cho trước (dùng cho CLIP search + filter). */
    public static Specification<Post> idIn(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return (root, query, cb) -> cb.disjunction();
        }
        return (root, query, cb) -> root.get("id").in(ids);
    }

    public static Specification<Post> hasType(PostType type) {
        if (type == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Post> hasStatus(PostStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Post> hasCategory(Category category) {
        if (category == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    /** District LIKE case-insensitive (join location). */
    public static Specification<Post> districtLike(String district) {
        if (district == null || district.trim().isEmpty()) {
            return null;
        }
        String clean = StringSanitizer.sanitizeSearchText(district);
        if (clean.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.like(
                cb.lower(root.join("location").get("district")),
                "%" + clean + "%");
    }

    /** Tag LIKE trên array_to_string(tags, ',') case-insensitive. */
    public static Specification<Post> tagLike(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return null;
        }
        String clean = StringSanitizer.sanitizeSearchText(tag);
        if (clean.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.like(
                cb.lower(cb.function("array_to_string", String.class, root.get("tags"), cb.literal(","))),
                "%" + clean + "%");
    }

    /**
     * Lọc eventTime theo date/time:
     * - Cả date + time: range +/- 1 giờ.
     * - Chỉ date: cả ngày (00:00:00 - 23:59:59).
     * - Chỉ time: trùng khớp giờ (date_part('hour', eventTime) = hour).
     */
    public static Specification<Post> eventTimeMatches(LocalDate date, LocalTime time) {
        if (date == null && time == null) {
            return null;
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (date != null && time != null) {
                LocalDateTime target = date.atTime(time);
                predicates.add(cb.between(root.get("eventTime"), target.minusHours(1), target.plusHours(1)));
            } else if (date != null) {
                predicates.add(cb.between(
                        root.get("eventTime"),
                        date.atStartOfDay(),
                        date.atTime(23, 59, 59)));
            } else {
                predicates.add(cb.equal(
                        cb.function("date_part", Integer.class, cb.literal("hour"), root.get("eventTime")),
                        time.getHour()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Gộp nhiều Specification lại (bỏ qua null). Trả về null nếu tất cả đều null
     * (tức không có filter nào).
     */
    public static Specification<Post> combine(Specification<Post>... specs) {
        Specification<Post> result = null;
        for (Specification<Post> spec : specs) {
            if (spec != null) {
                result = (result == null) ? spec : result.and(spec);
            }
        }
        return result;
    }
}
