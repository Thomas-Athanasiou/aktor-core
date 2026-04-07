package com.aktor.core.model;

import static java.util.Map.entry;

import com.aktor.core.ConditionType;
import com.aktor.core.FilterGroup;
import com.aktor.core.Model;
import com.aktor.core.Row;
import com.aktor.core.SearchCriteria;
import com.aktor.core.util.DataRowUtil;

import java.util.Map;
import java.util.Objects;

public final class SearchCriteriaCondition
implements Model
{
    private static final FilterGroupCondition DEFAULT_FILTER_GROUP_CONDITION = createDefaultFilterGroupCondition();

    private final FilterGroupCondition filterGroupCondition;

    SearchCriteriaCondition(final FilterGroupCondition filterGroupCondition)
    {
        super();
        this.filterGroupCondition = Objects.requireNonNull(filterGroupCondition);
    }

    public SearchCriteriaCondition()
    {
        this(DEFAULT_FILTER_GROUP_CONDITION);
    }

    public boolean isEntityMatch(final Row row, final SearchCriteria searchCriteria)
    {
        return isEntityMatch(DataRowUtil.toFieldMap(row), searchCriteria);
    }

    public boolean isEntityMatch(final Map<String, String> fieldMap, final SearchCriteria searchCriteria)
    {
        final FilterGroup[] filterGroups = searchCriteria.filterGroups();
        if (filterGroups.length < 1)
        {
            return true;
        }
        for (final FilterGroup filterGroup : filterGroups)
        {
            if (!filterGroupCondition.isEntityMatch(fieldMap, filterGroup))
            {
                return false;
            }
        }
        return true;
    }

    private static FilterGroupCondition createDefaultFilterGroupCondition()
    {
        return new FilterGroupCondition(
            new FilterConditionComposite(
                Map.ofEntries(
                    entry(ConditionType.EQUALS, new FilterConditionFieldPredicate((fieldValue, filterValue) -> fieldValue != null && fieldValue.equals(filterValue))),
                    entry(ConditionType.NOT_EQUALS, new FilterConditionFieldPredicate((fieldValue, filterValue) -> fieldValue != null && !fieldValue.equals(filterValue))),
                    entry(ConditionType.LIKE, new FilterConditionPatternPredicate(true)),
                    entry(ConditionType.NOT_LIKE, new FilterConditionPatternPredicate(false)),
                    entry(ConditionType.GREATER_THAN, new FilterConditionComparison(comparison -> comparison > 0)),
                    entry(ConditionType.GREATER_THAN_OR_EQUALS, new FilterConditionComparison(comparison -> comparison >= 0)),
                    entry(ConditionType.LESS_THAN, new FilterConditionComparison(comparison -> comparison < 0)),
                    entry(ConditionType.LESS_THAN_OR_EQUALS, new FilterConditionComparison(comparison -> comparison <= 0)),
                    entry(ConditionType.IN, new FilterConditionMembershipPredicate(true)),
                    entry(ConditionType.NOT_IN, new FilterConditionMembershipPredicate(false)),
                    entry(ConditionType.FROM, new FilterConditionComparison(comparison -> comparison >= 0)),
                    entry(ConditionType.TO, new FilterConditionComparison(comparison -> comparison <= 0)),
                    entry(ConditionType.VALUE_IN_SET, new FilterConditionValueSetPredicate(true)),
                    entry(ConditionType.VALUE_NOT_IN_SET, new FilterConditionValueSetPredicate(false)),
                    entry(ConditionType.IS_NULL, new FilterConditionIsNull()),
                    entry(ConditionType.IS_NOT_NULL, new FilterConditionIsNotNull()),
                    entry(ConditionType.MORE_OR_EQUALS, new FilterConditionComparison(comparison -> comparison >= 0))
                )
            )
        );
    }
}
