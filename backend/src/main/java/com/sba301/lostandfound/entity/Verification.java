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
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "verifications")
@Builder
@AllArgsConstructor
public class Verification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Builder.Default
    @Column(columnDefinition = "text")
    private String title = "AI Verification";

    private Integer importantPoint;

    // === Câu hỏi do AI sinh ra từ ảnh (V5 migration) ===

    /** Nội dung câu hỏi xác minh do AI (Ollama + Qwen-VL) sinh từ ảnh. */
    @Column(columnDefinition = "text")
    private String question;

    /**
     * Loại câu hỏi: TEXT (nhập tự do), MULTIPLE_CHOICE (chọn 1 trong options),
     * BOOLEAN (có/không).
     */
    @Column(name = "question_type", length = 20)
    private String questionType;

    /** Thứ tự câu hỏi trong bộ câu hỏi của post (0, 1, 2, ...). */
    @Column(name = "question_index")
    private Integer questionIndex;

    /**
     * JSON array options cho MULTIPLE_CHOICE. Format: ["Có","Không","Không rõ"].
     * Lưu dạng TEXT thay vì JSONB để đơn giản hoá migration và tương thích nhiều DB.
     */
    @Column(columnDefinition = "text")
    private String options;
}
