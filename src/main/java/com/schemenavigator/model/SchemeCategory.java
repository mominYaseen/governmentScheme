package com.schemenavigator.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scheme_categories", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"scheme_id", "category_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheme_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Scheme scheme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Category category;
}
