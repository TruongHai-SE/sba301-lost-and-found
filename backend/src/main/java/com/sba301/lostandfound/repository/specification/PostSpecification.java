package com.sba301.lostandfound.repository.specification;

import com.sba301.lostandfound.dto.PostFilterRequest;
import com.sba301.lostandfound.entity.Post;
import com.sba301.lostandfound.entity.enums.HidePostType;
import com.sba301.lostandfound.entity.enums.PostStatus;
import com.sba301.lostandfound.entity.enums.PostType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class PostSpecification {

    public static Specification<Post> withFilter(PostFilterRequest filter, PostType type, PostStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Only show public posts by default for search/filter
            predicates.add(cb.or(
                    cb.isNull(root.get("hidePostType")),
                    cb.equal(root.get("hidePostType"), HidePostType.PUBLIC)
            ));

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (filter != null) {
                if (filter.getDistrict() != null && !filter.getDistrict().trim().isEmpty()) {
                    predicates.add(cb.like(
                            cb.lower(root.join("location").get("district")),
                            "%" + filter.getDistrict().trim().toLowerCase() + "%"
                    ));
                }

                if (filter.getDate() != null && filter.getTime() != null) {
                    // Both Date and Time are present: range +/- 1 hour
                    LocalDateTime targetDateTime = filter.getDate().atTime(filter.getTime());
                    LocalDateTime start = targetDateTime.minusHours(1);
                    LocalDateTime end = targetDateTime.plusHours(1);
                    predicates.add(cb.between(root.get("eventTime"), start, end));
                } else if (filter.getDate() != null) {
                    // Only Date is present: whole day
                    LocalDateTime startOfDay = filter.getDate().atStartOfDay();
                    LocalDateTime endOfDay = filter.getDate().atTime(LocalTime.MAX);
                    predicates.add(cb.between(root.get("eventTime"), startOfDay, endOfDay));
                } else if (filter.getTime() != null) {
                    // Only Time is present: match by hour
                    predicates.add(cb.equal(
                            cb.function("EXTRACT", Integer.class, cb.literal("hour"), root.get("eventTime")),
                            filter.getTime().getHour()
                    ));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
