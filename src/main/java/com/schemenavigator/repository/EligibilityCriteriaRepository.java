package com.schemenavigator.repository;

import com.schemenavigator.model.EligibilityCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EligibilityCriteriaRepository extends JpaRepository<EligibilityCriteria, Long> {

    List<EligibilityCriteria> findBySchemeId(String schemeId);

    void deleteBySchemeId(String schemeId);
}
