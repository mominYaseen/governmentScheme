package com.schemenavigator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "UserInputRequest",
        description = "Natural-language description of the citizen’s situation for the AI matching flow (`POST /api/schemes/match`).")
public class UserInputRequest {

    @NotBlank(message = "User input cannot be empty")
    @Size(min = 3, max = 2000, message = "Input must be between 3 and 2000 characters")
    @Schema(
            description = "Free-text profile: who they are, where they live, income hints, occupation, etc.",
            example = "I am a woman farmer in Baramulla, J&K, annual income about 1.5 lakh, SC category.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 3,
            maxLength = 2000)
    private String userInput;

    @Schema(
            description = "Force response language for Gemini-generated strings: en | hi | ur | ks. Optional.",
            example = "en",
            allowableValues = {"en", "hi", "ur", "ks"},
            nullable = true)
    private String language;
}
