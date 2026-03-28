package com.schemenavigator.repository;

import com.schemenavigator.model.SchemeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemeCategoryRepository extends JpaRepository<SchemeCategory, Long> {

    boolean existsBySchemeIdAndCategoryId(String schemeId, Long categoryId);
}
