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
@Table(name = "verification_responses")
public class VerificationResponseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long claimId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_id")
    private VerificationJpaEntity verification;

    @Column(columnDefinition = "text")
    private String answer;

    private Double score;

    protected VerificationResponseJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public Long getClaimId() {
        return claimId;
    }

    public VerificationJpaEntity getVerification() {
        return verification;
    }

    public String getAnswer() {
        return answer;
    }

    public Double getScore() {
        return score;
    }
}
