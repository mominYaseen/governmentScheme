package com.schemenavigator.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "eligibility_criteria")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Scheme scheme;

    @Column(name = "min_income_annual")
    private Long minIncomeAnnual;

    @Column(name = "max_income_annual")
    private Long maxIncomeAnnual;

    /** Comma-separated uppercase state codes (e.g. JK,MP); null = any state */
    @Column(name = "state_codes", length = 512)
    private String stateCodes;

    /** Comma-separated lowercase occupation tokens; null = any */
    @Column(name = "occupations", length = 512)
    private String occupations;

    /** male / female / null = any */
    @Column(length = 16)
    private String gender;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
