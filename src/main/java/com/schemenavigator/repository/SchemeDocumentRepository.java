package com.schemenavigator.repository;

import com.schemenavigator.model.SchemeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchemeDocumentRepository extends JpaRepository<SchemeDocument, Long> {

    @Query("SELECT d FROM SchemeDocument d WHERE d.scheme.id = :schemeId")
    List<SchemeDocument> findBySchemeId(@Param("schemeId") String schemeId);
}
