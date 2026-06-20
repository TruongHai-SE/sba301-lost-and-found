package com.sba301.lostandfound.controller;

import com.sba301.lostandfound.dto.ApiResponse;
import com.sba301.lostandfound.dto.ClaimAnswerRequest;
import com.sba301.lostandfound.dto.ClaimResponse;
import com.sba301.lostandfound.dto.VerificationQuestionsResponse;
import com.sba301.lostandfound.service.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints phục vụ flow verification (AI hỏi - User trả lời).
 *
 *  - GET  /api/v1/posts/{postId}/verifications  : lấy câu hỏi AI sinh
 *  - POST /api/v1/posts/{postId}/claim          : claim post (verify ownership)
 *
 * <p>Flow:
 *  1. User search → nhận BlurredPostSummary (ảnh mờ) kèm câu hỏi.
 *  2. User trả lời câu hỏi → POST /claim.
 *  3. Server score: nếu >= threshold → trả FullPostDetails (ảnh rõ + liên hệ).
 *     Nếu < threshold → trả 403 với thông tin score.
 */
@RestController
@RequestMapping("/api/v1/posts/{postId}")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    /**
     * Lấy danh sách câu hỏi xác minh do AI sinh từ ảnh.
     * Nếu AI chưa kịp sinh → trả về status=NOT_GENERATED (FE sẽ retry sau vài giây).
     */
    @GetMapping("/verifications")
    public ResponseEntity<ApiResponse<VerificationQuestionsResponse>> getQuestions(@PathVariable Long postId) {
        VerificationQuestionsResponse result = verificationService.getQuestionsForPost(postId);
        return ResponseEntity.ok(ApiResponse.success(result, "Verification questions retrieved successfully"));
    }

    /**
     * Claim post: verify ownership bằng cách trả lời câu hỏi xác minh.
     *
     * <p>Response:
     * <ul>
     *   <li>200 OK: approved=true, body có {@code details} (ảnh rõ + thông tin liên hệ)</li>
     *   <li>403 FORBIDDEN: approved=false, body có score + message lý do từ chối</li>
     * </ul>
     */
    @PostMapping("/claim")
    public ResponseEntity<ApiResponse<ClaimResponse>> claim(
            @PathVariable Long postId,
            @Valid @RequestBody ClaimAnswerRequest request
    ) {
        ClaimResponse response = verificationService.claim(postId, request);
        if (!response.approved()) {
            // Throw để GlobalExceptionHandler trả 403 với body là ApiResponse<ClaimResponse>
            throw new ClaimRejectedException(response);
        }
        return ResponseEntity.ok(ApiResponse.success(response, "Claim approved successfully"));
    }

    /**
     * Exception nội bộ: dùng để trigger HTTP 403 với body JSON là ApiResponse<ClaimResponse>.
     * GlobalExceptionHandler sẽ bắt và convert.
     */
    public static class ClaimRejectedException extends RuntimeException {
        private final ClaimResponse response;
        public ClaimRejectedException(ClaimResponse response) {
            super(response.message());
            this.response = response;
        }
        public ClaimResponse getResponse() {
            return response;
        }
    }
}
