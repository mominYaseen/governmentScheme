package com.schemenavigator.repository;

import com.schemenavigator.model.UserSavedScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSavedSchemeRepository extends JpaRepository<UserSavedScheme, Long> {

    @Query("""
            SELECT uss FROM UserSavedScheme uss
            JOIN FETCH uss.scheme
            WHERE uss.user.id = :userId
            ORDER BY uss.createdAt DESC
            """)
    List<UserSavedScheme> findByUserIdWithSchemes(@Param("userId") Long userId);

    Optional<UserSavedScheme> findByUser_IdAndScheme_Id(Long userId, String schemeId);

    void deleteByUser_IdAndScheme_Id(Long userId, String schemeId);

    @Query("""
            SELECT DISTINCT uss FROM UserSavedScheme uss
            JOIN FETCH uss.user usr
            JOIN FETCH uss.scheme sch
            WHERE uss.remindEnabled = true
              AND uss.nextReminderAt IS NOT NULL
              AND uss.nextReminderAt <= :now
            """)
    List<UserSavedScheme> findDueForReminder(@Param("now") Instant now);
}
