package com.schemenavigator.repository;

import com.schemenavigator.model.Scheme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeRepository extends JpaRepository<Scheme, String> {

    @Query("SELECT DISTINCT s FROM Scheme s "
            + "LEFT JOIN FETCH s.eligibilityRules "
            + "LEFT JOIN FETCH s.documents "
            + "WHERE s.isActive = true")
    List<Scheme> findAllActiveWithRules();

    List<Scheme> findByIsActiveTrue();

    Page<Scheme> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT DISTINCT s FROM Scheme s LEFT JOIN FETCH s.documents WHERE s.id = :id")
    Optional<Scheme> findByIdWithDocuments(@Param("id") String id);

    long countBySource(String source);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM schemes WHERE source = :source", nativeQuery = true)
    int deleteBySource(@Param("source") String source);

    @Query("SELECT DISTINCT s FROM Scheme s JOIN FETCH s.eligibilityCriteria ec WHERE s.source = :source AND s.isActive = true")
    List<Scheme> findActiveBySourceWithCriteria(@Param("source") String source);

    @Query("SELECT s.id, c.name FROM Scheme s JOIN s.schemeCategories sc JOIN sc.category c "
            + "WHERE s.id IN :ids ORDER BY s.id ASC, c.name ASC")
    List<Object[]> findCategoryNamesBySchemeIds(@Param("ids") Collection<String> ids);
}
