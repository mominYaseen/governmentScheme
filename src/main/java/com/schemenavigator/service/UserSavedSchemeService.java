package com.schemenavigator.service;

import com.schemenavigator.dto.SaveSchemeRequest;
import com.schemenavigator.dto.SavedSchemeItemDto;
import com.schemenavigator.dto.SchemeSummaryDto;
import com.schemenavigator.model.AppUser;
import com.schemenavigator.model.Scheme;
import com.schemenavigator.model.UserSavedScheme;
import com.schemenavigator.repository.AppUserRepository;
import com.schemenavigator.repository.SchemeRepository;
import com.schemenavigator.repository.UserSavedSchemeRepository;
import com.schemenavigator.config.ReminderProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSavedSchemeService {

    private final UserSavedSchemeRepository userSavedSchemeRepository;
    private final AppUserRepository appUserRepository;
    private final SchemeRepository schemeRepository;
    private final SchemeCatalogService schemeCatalogService;
    private final ReminderProperties reminderProperties;

    @Transactional(readOnly = true)
    public List<SavedSchemeItemDto> listSaved(AppUser user) {
        List<UserSavedScheme> rows = userSavedSchemeRepository.findByUserIdWithSchemes(user.getId());
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Scheme> schemes = rows.stream().map(UserSavedScheme::getScheme).toList();
        List<SchemeSummaryDto> summaries = schemeCatalogService.summarizeSchemes(schemes);
        List<SavedSchemeItemDto> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            UserSavedScheme row = rows.get(i);
            out.add(new SavedSchemeItemDto(
                    row.getId(),
                    row.getCreatedAt(),
                    row.isRemindEnabled(),
                    row.getNextReminderAt(),
                    summaries.get(i)));
        }
        return out;
    }

    @Transactional
    public SavedSchemeItemDto save(AppUser user, SaveSchemeRequest request) {
        Scheme scheme = schemeRepository.findById(request.schemeId())
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scheme not found"));

        boolean remind = request.remindEnabled() == null || request.remindEnabled();
        Instant nextReminder = remind ? Instant.now().plus(reminderProperties.getIntervalDays(), ChronoUnit.DAYS) : null;

        UserSavedScheme row = userSavedSchemeRepository
                .findByUser_IdAndScheme_Id(user.getId(), scheme.getId())
                .map(existing -> {
                    existing.setRemindEnabled(remind);
                    if (remind && existing.getNextReminderAt() == null) {
                        existing.setNextReminderAt(nextReminder);
                    }
                    if (!remind) {
                        existing.setNextReminderAt(null);
                    }
                    return userSavedSchemeRepository.save(existing);
                })
                .orElseGet(() -> userSavedSchemeRepository.save(UserSavedScheme.builder()
                        .user(appUserRepository.getReferenceById(user.getId()))
                        .scheme(scheme)
                        .remindEnabled(remind)
                        .nextReminderAt(nextReminder)
                        .build()));

        SchemeSummaryDto summary = schemeCatalogService.summarizeSchemes(List.of(scheme)).getFirst();
        return new SavedSchemeItemDto(
                row.getId(),
                row.getCreatedAt(),
                row.isRemindEnabled(),
                row.getNextReminderAt(),
                summary);
    }

    @Transactional
    public void remove(AppUser user, String schemeId) {
        userSavedSchemeRepository.deleteByUser_IdAndScheme_Id(user.getId(), schemeId);
    }
}
