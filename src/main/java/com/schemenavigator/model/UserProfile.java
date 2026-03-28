package com.schemenavigator.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UserProfile", description = "Structured citizen profile used in match responses and internal processing.")
public class UserProfile {

    @Schema(description = "Normalized occupation token", example = "farmer", nullable = true)
    private String occupation;

    @Schema(description = "Annual income in Indian Rupees (whole rupees)", example = "150000", nullable = true)
    private Long incomeAnnual;

    @Schema(description = "City or district as mentioned by the user", example = "Srinagar", nullable = true)
    private String location;

    @Schema(description = "Two-letter state/UT code (e.g. JK for Jammu & Kashmir)", example = "JK", nullable = true)
    private String state;

    @Schema(description = "male | female | other", example = "female", nullable = true)
    private String gender;

    @Schema(description = "Whether the user owns land", nullable = true)
    private Boolean landOwned;

    @Schema(description = "GEN | OBC | SC | ST", example = "OBC", nullable = true)
    private String casteCategory;

    @Schema(description = "Age in full years", example = "32", nullable = true)
    private Integer age;

    @Schema(description = "Holds a BPL card", nullable = true)
    private Boolean bplCard;

    @Schema(description = "Farmer flag (may mirror occupation)", nullable = true)
    private Boolean isFarmer;

    @Schema(description = "Currently a student", nullable = true)
    private Boolean isStudent;

    @Schema(description = "Person with disability", nullable = true)
    private Boolean isDisabled;

    @Schema(description = "Original natural-language input (AI flow)", nullable = true)
    private String rawInput;

    @Schema(description = "UI / explanation language code: en | hi | ur | ks", example = "en")
    private String detectedLanguage;
}
