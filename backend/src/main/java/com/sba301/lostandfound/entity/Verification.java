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

    @Column(columnDefinition = "text")
    private String title;

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

    public Verification() {
    }

    /**
     * Convenience constructor cho PostAiEnrichmentService khi tạo câu hỏi từ AI.
     */
    public Verification(Post post, String question, String questionType, Integer questionIndex,
                        String options, Integer importantPoint) {
        this.post = post;
        this.question = question;
        this.questionType = questionType;
        this.questionIndex = questionIndex;
        this.options = options;
        this.importantPoint = importantPoint;
        this.title = "AI Verification";
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

    public String getQuestion() {
        return question;
    }

    public String getQuestionType() {
        return questionType;
    }

    public Integer getQuestionIndex() {
        return questionIndex;
    }

    public String getOptions() {
        return options;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setImportantPoint(Integer importantPoint) {
        this.importantPoint = importantPoint;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public void setQuestionIndex(Integer questionIndex) {
        this.questionIndex = questionIndex;
    }

    public void setOptions(String options) {
        this.options = options;
    }
}
