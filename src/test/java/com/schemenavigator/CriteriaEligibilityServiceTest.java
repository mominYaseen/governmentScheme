package com.schemenavigator;

import com.schemenavigator.config.DatasetImportProperties;
import com.schemenavigator.model.EligibilityCriteria;
import com.schemenavigator.model.Scheme;
import com.schemenavigator.model.UserProfile;
import com.schemenavigator.repository.SchemeRepository;
import com.schemenavigator.service.CriteriaEligibilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CriteriaEligibilityServiceTest {

    @Mock
    private SchemeRepository schemeRepository;

    private CriteriaEligibilityService service;

    @BeforeEach
    void setUp() {
        DatasetImportProperties props = new DatasetImportProperties();
        props.setSourceTag("KAGGLE_CSV");
        service = new CriteriaEligibilityService(schemeRepository, props);
    }

    @Test
    void rowMatchesWhenIncomeStateAndOccupationFit() {
        Scheme scheme = new Scheme();
        scheme.setId("x");
        EligibilityCriteria c = EligibilityCriteria.builder()
                .scheme(scheme)
                .maxIncomeAnnual(300_000L)
                .stateCodes("JK")
                .occupations("farmer")
                .build();
        scheme.setEligibilityCriteria(new LinkedHashSet<>(Set.of(c)));

        UserProfile ok = UserProfile.builder()
                .incomeAnnual(200_000L)
                .state("JK")
                .occupation("farmer")
                .build();
        assertTrue(service.rowMatches(ok, c));

        UserProfile highIncome = UserProfile.builder()
                .incomeAnnual(500_000L)
                .state("JK")
                .occupation("farmer")
                .build();
        assertFalse(service.rowMatches(highIncome, c));
    }

    @Test
    void genderRequiredWhenSetOnCriteria() {
        EligibilityCriteria c = EligibilityCriteria.builder()
                .gender("female")
                .build();
        UserProfile f = UserProfile.builder().gender("female").build();
        UserProfile m = UserProfile.builder().gender("male").build();
        assertTrue(service.rowMatches(f, c));
        assertFalse(service.rowMatches(m, c));
    }

    @Test
    void findEligibleSchemesReturnsMatchingOnly() {
        Scheme a = new Scheme();
        a.setId("a");
        EligibilityCriteria ca = EligibilityCriteria.builder().scheme(a).maxIncomeAnnual(100_000L).build();
        a.setEligibilityCriteria(new LinkedHashSet<>(Set.of(ca)));

        Scheme b = new Scheme();
        b.setId("b");
        EligibilityCriteria cb = EligibilityCriteria.builder().scheme(b).maxIncomeAnnual(500_000L).build();
        b.setEligibilityCriteria(new LinkedHashSet<>(Set.of(cb)));

        when(schemeRepository.findActiveBySourceWithCriteria(anyString())).thenReturn(List.of(a, b));

        UserProfile p = UserProfile.builder().incomeAnnual(200_000L).build();
        List<Scheme> out = service.findEligibleSchemes(p);
        assertEquals(1, out.size());
        assertEquals("b", out.get(0).getId());
    }
}
