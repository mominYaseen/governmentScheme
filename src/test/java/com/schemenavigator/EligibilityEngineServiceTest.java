package com.schemenavigator;

import com.schemenavigator.model.EligibilityRule;
import com.schemenavigator.model.MatchResult;
import com.schemenavigator.model.Scheme;
import com.schemenavigator.model.UserProfile;
import com.schemenavigator.service.EligibilityEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EligibilityEngineServiceTest {

    private EligibilityEngineService engine;

    @BeforeEach
    void setUp() {
        engine = new EligibilityEngineService();
    }

    @Test
    void eligibleWhenFarmerAndIncomeWithinPmKisanLimits() {
        Scheme scheme = new Scheme();
        scheme.setId("PM-KISAN");

        EligibilityRule occ = rule(scheme, "occupation", "EQUALS", "farmer", null, null, true, "not farmer");
        EligibilityRule inc = rule(scheme, "income_annual", "LESS_THAN_OR_EQUAL", null, BigDecimal.valueOf(200_000), null, true, "income too high");
        scheme.setEligibilityRules(new ArrayList<>(List.of(occ, inc)));

        UserProfile profile = UserProfile.builder()
                .occupation("farmer")
                .incomeAnnual(150_000L)
                .build();

        MatchResult result = engine.evaluate(profile, scheme);
        assertTrue(result.isEligible());
        assertEquals(2, result.getPassedRules().size());
        assertTrue(result.getFailedRules().isEmpty());
    }

    @Test
    void notEligibleWhenIncomeExceedsRule() {
        Scheme scheme = new Scheme();
        scheme.setId("PM-KISAN");

        EligibilityRule occ = rule(scheme, "occupation", "EQUALS", "farmer", null, null, true, "not farmer");
        EligibilityRule inc = rule(scheme, "income_annual", "LESS_THAN_OR_EQUAL", null, BigDecimal.valueOf(200_000), null, true, "income too high");
        scheme.setEligibilityRules(new ArrayList<>(List.of(occ, inc)));

        UserProfile profile = UserProfile.builder()
                .occupation("farmer")
                .incomeAnnual(300_000L)
                .build();

        MatchResult result = engine.evaluate(profile, scheme);
        assertFalse(result.isEligible());
        assertFalse(result.getFailedRules().isEmpty());
    }

    @Test
    void inOperatorMatchesVendor() {
        Scheme scheme = new Scheme();
        scheme.setId("PM-SVANidhi");
        EligibilityRule r = rule(scheme, "occupation", "IN", "street_vendor,vendor,hawker", null, null, true, "not vendor");
        scheme.setEligibilityRules(new ArrayList<>(List.of(r)));

        UserProfile profile = UserProfile.builder().occupation("vendor").build();
        MatchResult result = engine.evaluate(profile, scheme);
        assertTrue(result.isEligible());
    }

    @Test
    void jkStateRequired() {
        Scheme scheme = new Scheme();
        scheme.setId("JKEDI-LOAN");
        EligibilityRule r = rule(scheme, "state", "EQUALS", "JK", null, null, true, "not JK");
        scheme.setEligibilityRules(new ArrayList<>(List.of(r)));

        UserProfile ok = UserProfile.builder().state("JK").build();
        assertTrue(engine.evaluate(ok, scheme).isEligible());

        UserProfile bad = UserProfile.builder().state("DL").build();
        assertFalse(engine.evaluate(bad, scheme).isEligible());
    }

    private static EligibilityRule rule(Scheme scheme, String field, String op, String vs, BigDecimal vn, Boolean vb,
                                        boolean mandatory, String fail) {
        EligibilityRule r = new EligibilityRule();
        r.setScheme(scheme);
        r.setFieldName(field);
        r.setOperator(op);
        r.setValueString(vs);
        r.setValueNumber(vn);
        r.setValueBoolean(vb);
        r.setIsMandatory(mandatory);
        r.setFailureMessage(fail);
        return r;
    }
}
