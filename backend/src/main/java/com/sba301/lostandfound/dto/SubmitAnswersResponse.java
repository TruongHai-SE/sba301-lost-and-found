package com.sba301.lostandfound.dto;

public record SubmitAnswersResponse(
    Long postId,
    int savedCount,
    String message
) {}
