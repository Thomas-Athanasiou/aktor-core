package com.aktor.core.service;

import com.aktor.core.ConditionType;
import com.aktor.core.FilterGroup;
import com.aktor.core.value.Filter;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FilterEvaluationServiceTest
{
    private final FilterEvaluationService service = new FilterEvaluationService();

    @Test
    public void isMatchAll_requiresEveryFilterToMatch()
    {
        final Map<String, String> row = Map.of(
            "status", "open",
            "amount", "11"
        );

        assertTrue(service.isMatchAll(
            row,
            new Filter("status", "open", ConditionType.EQUALS),
            new Filter("amount", "10", ConditionType.GREATER_THAN_OR_EQUALS),
            new Filter("amount", "15", ConditionType.LESS_THAN_OR_EQUALS)
        ));
        assertFalse(service.isMatchAll(
            row,
            new Filter("status", "closed", ConditionType.EQUALS),
            new Filter("amount", "10", ConditionType.GREATER_THAN_OR_EQUALS)
        ));
    }

    @Test
    public void isMatch_supportsGroupedCriteria()
    {
        final Map<String, String> row = Map.of(
            "status", "open",
            "symbol", "AAPL"
        );

        assertTrue(service.isMatch(
            row,
            FilterEvaluationService.criteria(
                new FilterGroup(new Filter[]
                {
                    new Filter("status", "open", ConditionType.EQUALS),
                    new Filter("status", "pending", ConditionType.EQUALS)
                }),
                new FilterGroup(new Filter[]
                {
                    new Filter("symbol", "AAP%", ConditionType.LIKE)
                })
            )
        ));
        assertFalse(service.isMatch(
            row,
            FilterEvaluationService.criteria(
                new FilterGroup(new Filter[]
                {
                    new Filter("status", "closed", ConditionType.EQUALS)
                }),
                new FilterGroup(new Filter[]
                {
                    new Filter("symbol", "AAP%", ConditionType.LIKE)
                })
            )
        ));
    }
}
