package com.sba301.lostandfound.infrastructure.persistence.entity;

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
public class CorrectAnswerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_id")
    private VerificationJpaEntity verification;

    @Column(nullable = false, columnDefinition = "text")
    private String answer;

    protected CorrectAnswerJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public VerificationJpaEntity getVerification() {
        return verification;
    }

    public String getAnswer() {
        return answer;
    }
}
