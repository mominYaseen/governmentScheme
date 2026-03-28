package com.schemenavigator.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "eligibility_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Scheme scheme;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(nullable = false)
    private String operator;

    @Column(name = "value_string")
    private String valueString;

    @Column(name = "value_number")
    private BigDecimal valueNumber;

    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    @Column(name = "is_mandatory")
    private Boolean isMandatory = true;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;
}
