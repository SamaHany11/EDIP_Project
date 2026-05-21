package com.example.EDIP.feedback.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

@Data
public class SubmitFeedbackRequest {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank
    @Size(max = 2000)
    private String feedbackContent;
}