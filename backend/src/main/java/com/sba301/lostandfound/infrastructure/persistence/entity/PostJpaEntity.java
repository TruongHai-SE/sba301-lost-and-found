package com.sba301.lostandfound.infrastructure.persistence.entity;

import com.sba301.lostandfound.domain.model.HidePostType;
import com.sba301.lostandfound.domain.model.PostStatus;
import com.sba301.lostandfound.domain.model.PostType;
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

@Entity
@Table(name = "posts")
public class PostJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserJpaEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private LocationJpaEntity location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private ImageJpaEntity image;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private PostType type;

    @Column(name = "event_time")
    private LocalDateTime eventTime;

    @Column(name = "create_at")
    private LocalDateTime createAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PostStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "hide_post_type", length = 10)
    private HidePostType hidePostType;

    @Column(name = "delete_at")
    private LocalDateTime deleteAt;

    protected PostJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public UserJpaEntity getUser() {
        return user;
    }

    public LocationJpaEntity getLocation() {
        return location;
    }

    public ImageJpaEntity getImage() {
        return image;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public PostType getType() {
        return type;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }

    public PostStatus getStatus() {
        return status;
    }

    public HidePostType getHidePostType() {
        return hidePostType;
    }

    public LocalDateTime getDeleteAt() {
        return deleteAt;
    }
}
