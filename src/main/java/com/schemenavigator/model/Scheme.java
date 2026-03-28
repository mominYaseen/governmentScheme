package com.schemenavigator.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "schemes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Scheme {
    @Id
    @Column(length = 128)
    private String id;

    @Column(nullable = false, length = 512)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String ministry;

    @Column(columnDefinition = "TEXT")
    private String benefits;

    @Column(name = "apply_process", columnDefinition = "TEXT")
    private String applyProcess;

    @Column(name = "apply_url")
    private String applyUrl;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(length = 128)
    private String slug;

    @Column(name = "gov_level", length = 32)
    private String govLevel;

    @Column(name = "eligibility_raw", columnDefinition = "TEXT")
    private String eligibilityRaw;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(length = 32)
    private String source;

    @OneToMany(mappedBy = "scheme", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<EligibilityRule> eligibilityRules = new LinkedHashSet<>();

    @OneToMany(mappedBy = "scheme", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<SchemeDocument> documents = new LinkedHashSet<>();

    @OneToMany(mappedBy = "scheme", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<EligibilityCriteria> eligibilityCriteria = new LinkedHashSet<>();

    @OneToMany(mappedBy = "scheme", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<SchemeCategory> schemeCategories = new LinkedHashSet<>();
}
