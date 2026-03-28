package com.schemenavigator.repository;

import com.schemenavigator.model.Scheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchemeRepository extends JpaRepository<Scheme, String> {

    @Query("SELECT DISTINCT s FROM Scheme s "
            + "LEFT JOIN FETCH s.eligibilityRules "
            + "LEFT JOIN FETCH s.documents "
            + "WHERE s.isActive = true")
    List<Scheme> findAllActiveWithRules();

    List<Scheme> findByIsActiveTrue();
}
