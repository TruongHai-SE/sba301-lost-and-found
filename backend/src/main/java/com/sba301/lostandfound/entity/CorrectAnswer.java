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
@Table(name = "correct_answers")
public class CorrectAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_id")
    private Verification verification;

    @Column(nullable = false, columnDefinition = "text")
    private String answer;

    protected CorrectAnswer() {
    }

    public Long getId() {
        return id;
    }

    public Verification getVerification() {
        return verification;
    }

    public String getAnswer() {
        return answer;
    }
}
