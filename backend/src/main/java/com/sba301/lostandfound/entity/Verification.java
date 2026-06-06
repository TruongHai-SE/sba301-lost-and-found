package com.sba301.lostandfound.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "verifications")
public class Verification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(columnDefinition = "text")
    private String title;

    private Integer importantPoint;

    protected Verification() {
    }

    public Long getId() {
        return id;
    }

    public Post getPost() {
        return post;
    }

    public String getTitle() {
        return title;
    }

    public Integer getImportantPoint() {
        return importantPoint;
    }
}
