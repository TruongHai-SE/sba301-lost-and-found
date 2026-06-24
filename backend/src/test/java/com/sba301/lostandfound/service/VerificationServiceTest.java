package com.sba301.lostandfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.sba301.lostandfound.dto.ClaimAnswerRequest;
import com.sba301.lostandfound.dto.ClaimResponse;
import com.sba301.lostandfound.dto.VerificationQuestionsResponse;
import com.sba301.lostandfound.entity.Post;
import com.sba301.lostandfound.entity.Verification;
import com.sba301.lostandfound.entity.VerificationAnswer;
import com.sba301.lostandfound.repository.PostRepository;
import com.sba301.lostandfound.repository.VerificationAnswerRepository;
import com.sba301.lostandfound.repository.VerificationRepository;
import com.sba301.lostandfound.service.impl.VerificationServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private VerificationRepository verificationRepository;

    @Mock
    private VerificationAnswerRepository verificationAnswerRepository;

    @Mock
    private PostRepository postRepository;

    private VerificationServiceImpl verificationService;

    @BeforeEach
    void setUp() {
        verificationService = new VerificationServiceImpl(
            verificationRepository,
            verificationAnswerRepository,
            postRepository
        );
    }

    private Post mockPost(Long id) {
        return Post.builder()
            .title("Test Post")
            .description("Description")
            .build();
    }

    private Verification mockVerification(Long id, String type) {
        Verification v = Verification.builder()
            .question("What color?")
            .questionType(type)
            .questionIndex(1)
            .build();
        try {
            var field = Verification.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(v, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return v;
    }

    private VerificationAnswer mockAnswer(Long verificationId, String answer) {
        return VerificationAnswer.builder()
            .answer(answer)
            .build();
    }

    // =========================================================================
    // getQuestionsForPost
    // =========================================================================

    @Test
    void getQuestionsForPost_notFound_throwsNotFound() {
        given(postRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> verificationService.getQuestionsForPost(99L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getQuestionsForPost_noQuestions_returnsEmptyResponse() {
        given(postRepository.existsById(1L)).willReturn(true);
        given(verificationRepository.findByPostIdOrderByQuestionIndexAsc(1L))
            .willReturn(List.of());

        VerificationQuestionsResponse result = verificationService.getQuestionsForPost(1L);

        assertThat(result.status()).isEqualTo("NOT_GENERATED");
        assertThat(result.questions()).isEmpty();
    }

    @Test
    void getQuestionsForPost_hasQuestions_returnsPendingResponse() {
        given(postRepository.existsById(1L)).willReturn(true);
        Verification v = mockVerification(10L, "TEXT");
        given(verificationRepository.findByPostIdOrderByQuestionIndexAsc(1L))
            .willReturn(List.of(v));

        VerificationQuestionsResponse result = verificationService.getQuestionsForPost(1L);

        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.questions()).hasSize(1);
        assertThat(result.questions().get(0).id()).isEqualTo(10L);
    }

    // =========================================================================
    // scoreAnswer & types
    // =========================================================================

    @Test
    void scoreAnswer_booleanCorrect_returnsOne() {
        given(verificationRepository.findById(10L))
            .willReturn(Optional.of(mockVerification(10L, "BOOLEAN")));
        given(verificationAnswerRepository.findFirstByVerificationId(10L))
            .willReturn(Optional.of(mockAnswer(10L, "có")));

        double score = verificationService.scoreAnswer(10L, "Có ");
        assertThat(score).isEqualTo(1.0);
    }

    @Test
    void scoreAnswer_booleanIncorrect_returnsZero() {
        given(verificationRepository.findById(10L))
            .willReturn(Optional.of(mockVerification(10L, "BOOLEAN")));
        given(verificationAnswerRepository.findFirstByVerificationId(10L))
            .willReturn(Optional.of(mockAnswer(10L, "có")));

        double score = verificationService.scoreAnswer(10L, "không");
        assertThat(score).isEqualTo(0.0);
    }

    @Test
    void scoreAnswer_multipleChoiceCorrect_returnsOne() {
        given(verificationRepository.findById(10L))
            .willReturn(Optional.of(mockVerification(10L, "MULTIPLE_CHOICE")));
        given(verificationAnswerRepository.findFirstByVerificationId(10L))
            .willReturn(Optional.of(mockAnswer(10L, "Option A")));

        double score = verificationService.scoreAnswer(10L, "option a");
        assertThat(score).isEqualTo(1.0);
    }

    @Test
    void scoreAnswer_textJaccardSimilarity_returnsPartialScore() {
        given(verificationRepository.findById(10L))
            .willReturn(Optional.of(mockVerification(10L, "TEXT")));
        given(verificationAnswerRepository.findFirstByVerificationId(10L))
            .willReturn(Optional.of(mockAnswer(10L, "Ví tiền màu nâu")));

        // Jaccard của "ví tiền màu nâu" (4 từ) và "ví tiền màu đỏ" (4 từ)
        // Giao: "ví", "tiền", "màu" (3)
        // Hợp: "ví", "tiền", "màu", "nâu", "đỏ" (5)
        // Điểm: 3/5 = 0.6
        double score = verificationService.scoreAnswer(10L, "ví tiền màu đỏ");
        assertThat(score).isEqualTo(0.6);
    }

    // =========================================================================
    // claim
    // =========================================================================

    @Test
    void claim_scoreAboveThreshold_returnsApproved() {
        Post post = mockPost(1L);
        Verification v = mockVerification(10L, "TEXT");

        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(verificationRepository.findByPostIdOrderByQuestionIndexAsc(1L))
            .willReturn(List.of(v));

        given(verificationRepository.findById(10L)).willReturn(Optional.of(v));
        given(verificationAnswerRepository.findFirstByVerificationId(10L))
            .willReturn(Optional.of(mockAnswer(10L, "Ví màu nâu")));

        ClaimAnswerRequest req = new ClaimAnswerRequest(
            List.of(new ClaimAnswerRequest.Answer(10L, "ví màu nâu"))
        );

        ClaimResponse result = verificationService.claim(1L, req);

        assertThat(result.approved()).isTrue();
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.details()).isNotNull();
    }

    @Test
    void claim_scoreBelowThreshold_returnsRejected() {
        Post post = mockPost(1L);
        Verification v = mockVerification(10L, "TEXT");

        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(verificationRepository.findByPostIdOrderByQuestionIndexAsc(1L))
            .willReturn(List.of(v));

        given(verificationRepository.findById(10L)).willReturn(Optional.of(v));
        given(verificationAnswerRepository.findFirstByVerificationId(10L))
            .willReturn(Optional.of(mockAnswer(10L, "Ví màu nâu")));

        ClaimAnswerRequest req = new ClaimAnswerRequest(
            List.of(new ClaimAnswerRequest.Answer(10L, "màu đỏ"))
        );

        ClaimResponse result = verificationService.claim(1L, req);

        assertThat(result.approved()).isFalse();
        assertThat(result.score()).isLessThan(0.6);
        assertThat(result.details()).isNull();
    }
}
