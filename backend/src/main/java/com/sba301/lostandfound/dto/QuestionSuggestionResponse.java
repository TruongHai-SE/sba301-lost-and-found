package com.sba301.lostandfound.dto;

import java.util.List;

public record QuestionSuggestionResponse(
    String imageUrl,
    List<OllamaQuestionsResponse.OllamaQuestion> questions
) {}
