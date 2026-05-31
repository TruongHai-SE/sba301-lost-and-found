package com.sba301.lostandfound.infrastructure.persistence.entity;

import com.sba301.lostandfound.domain.model.MatchRequestStatus;
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
@Table(name = "match_requests")
public class MatchRequestJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserJpaEntity user;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private MatchRequestStatus status;

    @Column(name = "create_at")
    private LocalDateTime createAt;

    @Column(columnDefinition = "text")
    private String message;

    protected MatchRequestJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public UserJpaEntity getUser() {
        return user;
    }

    public MatchRequestStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }

    public String getMessage() {
        return message;
    }
}
