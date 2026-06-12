package com.sba301.lostandfound.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerificationQuestionRequest {
    private String title;
    private String correctAnswer;
    private Integer importantPoint;
}