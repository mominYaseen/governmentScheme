package com.schemenavigator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInputRequest {
    @NotBlank(message = "User input cannot be empty")
    @Size(min = 3, max = 2000, message = "Input must be between 3 and 2000 characters")
    private String userInput;

    private String language;
}
