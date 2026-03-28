package com.schemenavigator.repository;

import com.schemenavigator.model.EligibilityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EligibilityRuleRepository extends JpaRepository<EligibilityRule, Long> {

    @Query("SELECT e FROM EligibilityRule e WHERE e.scheme.id = :schemeId")
    List<EligibilityRule> findBySchemeId(@Param("schemeId") String schemeId);
}
