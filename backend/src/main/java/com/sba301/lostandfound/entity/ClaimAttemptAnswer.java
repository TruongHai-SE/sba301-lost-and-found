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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thực thể lưu trữ câu trả lời của người claim đồ (claim attempt answer).
 *
 * <p>Thay thế cho tên cũ {@code VerificationResponse} để tránh nhầm lẫn với các đối tượng DTO.
 */
@Entity
@Table(name = "claim_attempt_answers")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ClaimAttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long claimId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_id")
    private Verification verification;

    @Column(columnDefinition = "text")
    private String answer;

    private Double score;
}
