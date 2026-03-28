package com.schemenavigator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private String occupation;
    private Long incomeAnnual;
    private String location;
    private String state;
    private String gender;
    private Boolean landOwned;
    private String casteCategory;
    private Integer age;
    private Boolean bplCard;
    private Boolean isFarmer;
    private Boolean isStudent;
    private Boolean isDisabled;
    private String rawInput;
    private String detectedLanguage;
}
