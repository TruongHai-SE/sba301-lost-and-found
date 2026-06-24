package com.sba301.lostandfound.entity;

import com.sba301.lostandfound.entity.enums.HidePostType;
import com.sba301.lostandfound.entity.enums.PostStatus;
import com.sba301.lostandfound.entity.enums.PostType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "posts")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private Image image;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private PostType type;

    @Column(name = "event_time")
    private LocalDateTime eventTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PostStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "hide_post_type", length = 10)
    private HidePostType hidePostType;

    @Column(name = "delete_at")
    private LocalDateTime deleteAt;

    // === AI enrichment (Ollama + Qwen-VL) ===
    @Column(name = "ai_description", columnDefinition = "text")
    private String aiDescription;

    @Column(name = "ai_tags", columnDefinition = "text")
    private String aiTags;

    @Column(name = "ai_enriched_at")
    private LocalDateTime aiEnrichedAt;
}
