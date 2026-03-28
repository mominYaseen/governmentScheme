package com.schemenavigator.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scheme_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemeDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Scheme scheme;

    @Column(name = "document_name", nullable = false)
    private String documentName;

    @Column(name = "is_mandatory")
    private Boolean isMandatory = true;

    private String description;
}
