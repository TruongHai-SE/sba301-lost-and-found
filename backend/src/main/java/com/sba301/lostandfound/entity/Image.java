package com.sba301.lostandfound.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "images")
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "text")
    private String url;

    @Column(name = "private_url", columnDefinition = "text")
    private String privateUrl;

    @Column(name = "create_at")
    private LocalDateTime createAt;

    protected Image() {
    }

    public Long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getPrivateUrl() {
        return privateUrl;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }
}
