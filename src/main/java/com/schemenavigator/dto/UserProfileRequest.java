package com.schemenavigator.dto;

import com.schemenavigator.model.UserProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JSON body for {@code POST /api/schemes/recommend} (structured profile).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "UserProfileRequest",
        description = """
                Structured profile for `POST /api/schemes/recommend`.
                Only schemes imported with parseable `eligibility_criteria` (CSV pipeline) are evaluated.
                Null fields mean “unknown”; constraints that require a value will fail matching for that criterion.
                """)
public class UserProfileRequest {

    @Schema(description = "Occupation token: farmer, student, vendor, labourer, …", example = "farmer", nullable = true)
    private String occupation;

    @Schema(description = "Annual income in INR", example = "200000", nullable = true)
    private Long incomeAnnual;

    @Schema(description = "City or district", example = "Jammu", nullable = true)
    private String location;

    @Schema(description = "Two-letter state/UT code", example = "JK", nullable = true)
    private String state;

    @Schema(description = "male | female | other", example = "female", nullable = true)
    private String gender;

    @Schema(nullable = true)
    private Boolean landOwned;

    @Schema(description = "GEN | OBC | SC | ST", example = "SC", nullable = true)
    private String casteCategory;

    @Schema(example = "28", nullable = true)
    private Integer age;

    @Schema(nullable = true)
    private Boolean bplCard;

    @Schema(nullable = true)
    private Boolean isFarmer;

    @Schema(nullable = true)
    private Boolean isStudent;

    @Schema(nullable = true)
    private Boolean isDisabled;

    public UserProfile toUserProfile() {
        return UserProfile.builder()
                .occupation(occupation)
                .incomeAnnual(incomeAnnual)
                .location(location)
                .state(state != null ? state.toUpperCase() : null)
                .gender(gender != null ? gender.toLowerCase() : null)
                .landOwned(landOwned)
                .casteCategory(casteCategory != null ? casteCategory.toUpperCase() : null)
                .age(age)
                .bplCard(bplCard)
                .isFarmer(isFarmer)
                .isStudent(isStudent)
                .isDisabled(isDisabled)
                .detectedLanguage("en")
                .build();
    }
}
