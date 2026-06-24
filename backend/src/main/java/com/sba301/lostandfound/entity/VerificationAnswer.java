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
 * Lưu đáp án chuẩn của câu hỏi xác minh ({@link Verification}).
 *
 * <p>Đây là đáp án mà chủ đồ vật (hoặc AI) cung cấp khi đăng bài FOUND.
 * Khi người khác claim, server sẽ so sánh câu trả lời của họ với entity này
 * để tính điểm xác minh.
 */
@Entity
@Table(name = "verification_answers")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class VerificationAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_id")
    private Verification verification;

    @Column(nullable = false, columnDefinition = "text")
    private String answer;
}
