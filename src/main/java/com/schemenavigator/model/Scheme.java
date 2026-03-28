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
    private String id;

    @Column(nullable = false)
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

    @OneToMany(mappedBy = "scheme", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<EligibilityRule> eligibilityRules = new LinkedHashSet<>();

    @OneToMany(mappedBy = "scheme", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<SchemeDocument> documents = new LinkedHashSet<>();
}
