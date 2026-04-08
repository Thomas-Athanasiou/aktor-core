package com.aktor.core.model;

import com.aktor.core.Converter;
import com.aktor.core.Data;
import com.aktor.core.Row;
import com.aktor.core.FilterGroup;
import com.aktor.core.SearchCriteria;
import com.aktor.core.SearchResult;
import com.aktor.core.SortOrder;
import com.aktor.core.exception.ConversionException;
import com.aktor.core.exception.SearchException;
import com.aktor.core.util.SimpleDataObjectConverter;
import com.aktor.core.value.Filter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class CollectionProcessor<Item extends Data<Key>, Key>
{
    private static final FilterGroup[] FILTER_GROUPS = new FilterGroup[0];

    private static final SortOrder[] SORT_ORDERS = new SortOrder[0];
    private static final PreparedSortOrder[] PREPARED_SORT_ORDERS = new PreparedSortOrder[0];
    private static final String[] SORT_VALUES = new String[0];

    private final SearchCriteriaCondition condition;

    private final Converter<Item, Row> mapper;

    public CollectionProcessor(final SearchCriteriaCondition condition, final Converter<Item, Row> mapper)
    {
        super();
        this.condition = Objects.requireNonNull(condition);
        this.mapper = Objects.requireNonNull(mapper);
    }

    public SearchResult<Item> process(final Iterable<Item> items, final SearchCriteria searchCriteria) throws SearchException
    {
        if (searchCriteria == null)
        {
            final List<Item> itemList = toList(items);
            return new SearchResult<>(
                itemList,
                new SearchCriteria(
                    FILTER_GROUPS,
                    Math.max(itemList.size(), 1),
                    1,
                    SORT_ORDERS
                ),
                itemList.size()
            );
        }

        try
        {
            final PreparedSearch preparedSearch = prepareSearch(searchCriteria);
            return preparedSearch.sortOrders().length < 1
                ? processUnsorted(items, preparedSearch)
                : processSorted(items, preparedSearch);
        }
        catch (final ConversionException exception)
        {
            throw new SearchException(exception);
        }
    }

    public List<Item> sort(final Collection<Item> list, final SearchCriteria searchCriteria) throws ConversionException
    {
        final PreparedSearch preparedSearch = prepareSearch(searchCriteria);
        final List<IndexedItem<Item>> indexedItems = new ArrayList<>(list.size());
        for (final Item item : list)
        {
            indexedItems.add(indexItem(item, preparedSearch));
        }
        indexedItems.sort(comparator(preparedSearch));

        final List<Item> result = new ArrayList<>(indexedItems.size());
        for (final IndexedItem<Item> indexedItem : indexedItems)
        {
            result.add(indexedItem.item());
        }
        return result;
    }

    private SearchResult<Item> processUnsorted(final Iterable<Item> items, final PreparedSearch preparedSearch)
    throws ConversionException
    {
        final SearchCriteria searchCriteria = preparedSearch.criteria();
        final int pageSize = searchCriteria.pageSize();
        final int offset = safeOffset(pageSize, searchCriteria.currentPage());
        final Set<Key> matchedKeys = new LinkedHashSet<>();
        final List<Item> pageItems = new ArrayList<>(Math.max(1, pageSize));

        for (final Item item : items)
        {
            final IndexedItem<Item> indexedItem = indexItem(item, preparedSearch);
            @SuppressWarnings("unchecked")
            final Key key = (Key) indexedItem.key();
            if (condition.isEntityMatch(indexedItem.fields(), searchCriteria) && matchedKeys.add(key))
            {
                final int matchIndex = matchedKeys.size() - 1;
                if (matchIndex >= offset && pageItems.size() < pageSize)
                {
                    pageItems.add(item);
                }
            }
        }

        return new SearchResult<>(pageItems, searchCriteria, matchedKeys.size());
    }

    private SearchResult<Item> processSorted(final Iterable<Item> items, final PreparedSearch preparedSearch)
    throws ConversionException
    {
        final SearchCriteria searchCriteria = preparedSearch.criteria();
        final Map<Key, IndexedItem<Item>> matchedItems = new LinkedHashMap<>();
        for (final Item item : items)
        {
            final IndexedItem<Item> indexedItem = indexItem(item, preparedSearch);
            @SuppressWarnings("unchecked")
            final Key key = (Key) indexedItem.key();
            if (condition.isEntityMatch(indexedItem.fields(), searchCriteria))
            {
                matchedItems.putIfAbsent(key, indexedItem);
            }
        }

        final List<IndexedItem<Item>> sortedItems = new ArrayList<>(matchedItems.values());
        sortedItems.sort(comparator(preparedSearch));

        final int fromIndex = safeOffset(searchCriteria.pageSize(), searchCriteria.currentPage(), sortedItems.size());
        final int toIndex = safeUpperBound(fromIndex, searchCriteria.pageSize(), sortedItems.size());
        final List<Item> pageItems = new ArrayList<>(Math.max(0, toIndex - fromIndex));
        for (int index = fromIndex; index < toIndex; index++)
        {
            pageItems.add(sortedItems.get(index).item());
        }

        return new SearchResult<>(pageItems, searchCriteria, sortedItems.size());
    }

    private IndexedItem<Item> indexItem(final Item item, final PreparedSearch preparedSearch) throws ConversionException
    {
        final Row row = mapper.convert(item);
        final Map<String, String> fields = Row.toFieldMap(row);
        final PreparedSortOrder[] sortOrders = preparedSearch.sortOrders();
        final String[] sortValues;
        if (sortOrders.length < 1)
        {
            sortValues = SORT_VALUES;
        }
        else
        {
            sortValues = new String[sortOrders.length];
            for (int index = 0; index < sortOrders.length; index++)
            {
                sortValues[index] = fields.get(sortOrders[index].field());
            }
        }
        return new IndexedItem<>(item, item.key(), fields, sortValues);
    }

    private Comparator<IndexedItem<Item>> comparator(final PreparedSearch preparedSearch)
    {
        return (left, right) -> {
            int result = 0;
            final PreparedSortOrder[] sortOrders = preparedSearch.sortOrders();
            for (int index = 0; index < sortOrders.length; index++)
            {
                final PreparedSortOrder order = sortOrders[index];
                final String valueLeft = left.sortValues()[index];
                final String valueRight = right.sortValues()[index];

                if (valueLeft == null)
                {
                    result = order.ascending() ? -1 : 1;
                }
                else if (valueRight == null)
                {
                    result = order.ascending() ? 1 : -1;
                }
                else
                {
                    final int comparison = compareSortValues(valueLeft, valueRight);
                    if (comparison != 0)
                    {
                        result = order.ascending() ? comparison : -comparison;
                    }
                }

                if (result != 0)
                {
                    break;
                }
            }
            if (result == 0)
            {
                result = compareByKey(left.key(), right.key());
            }
            return result;
        };
    }

    private static PreparedSearch prepareSearch(final SearchCriteria searchCriteria)
    {
        final FilterGroup[] filterGroups = searchCriteria.filterGroups();
        final FilterGroup[] preparedGroups;
        if (filterGroups.length < 1)
        {
            preparedGroups = FILTER_GROUPS;
        }
        else
        {
            preparedGroups = new FilterGroup[filterGroups.length];
            for (int groupIndex = 0; groupIndex < filterGroups.length; groupIndex++)
            {
                final Filter[] filters = filterGroups[groupIndex].filters();
                final Filter[] preparedFilters = new Filter[filters.length];
                for (int filterIndex = 0; filterIndex < filters.length; filterIndex++)
                {
                    final Filter filter = filters[filterIndex];
                    preparedFilters[filterIndex] = new Filter(
                        normalizeField(filter.field()),
                        filter.value(),
                        filter.conditionType()
                    );
                }
                preparedGroups[groupIndex] = new FilterGroup(preparedFilters);
            }
        }

        final SortOrder[] sortOrders = searchCriteria.sortOrders();
        final PreparedSortOrder[] preparedSortOrders;
        final SortOrder[] normalizedSortOrders;
        if (sortOrders.length < 1)
        {
            preparedSortOrders = PREPARED_SORT_ORDERS;
            normalizedSortOrders = SORT_ORDERS;
        }
        else
        {
            preparedSortOrders = new PreparedSortOrder[sortOrders.length];
            normalizedSortOrders = new SortOrder[sortOrders.length];
            for (int index = 0; index < sortOrders.length; index++)
            {
                final SortOrder order = sortOrders[index];
                final String normalizedField = normalizeField(order.field());
                preparedSortOrders[index] = new PreparedSortOrder(normalizedField, order.direction());
                normalizedSortOrders[index] = new SortOrder(normalizedField, order.direction());
            }
        }

        return new PreparedSearch(
            new SearchCriteria(preparedGroups, searchCriteria.pageSize(), searchCriteria.currentPage(), normalizedSortOrders),
            preparedSortOrders
        );
    }

    private static String normalizeField(final CharSequence field)
    {
        return SimpleDataObjectConverter.camelToSnake(field).toLowerCase(Locale.ROOT);
    }

    private static int compareSortValues(final String left, final String right)
    {
        final Long leftLong = parseLongOrNull(left);
        final Long rightLong = parseLongOrNull(right);
        if (leftLong != null && rightLong != null)
        {
            return Long.compare(leftLong, rightLong);
        }

        final Double leftDouble = parseDoubleOrNull(left);
        final Double rightDouble = parseDoubleOrNull(right);
        if (leftDouble != null && rightDouble != null)
        {
            return Double.compare(leftDouble, rightDouble);
        }

        final Long leftTime = parseTimeMillisOrNull(left);
        final Long rightTime = parseTimeMillisOrNull(right);
        if (leftTime != null && rightTime != null)
        {
            return Long.compare(leftTime, rightTime);
        }

        return left.compareToIgnoreCase(right);
    }

    private static int compareByKey(final Object leftKey, final Object rightKey)
    {
        final String left = leftKey == null ? "" : leftKey.toString();
        final String right = rightKey == null ? "" : rightKey.toString();
        return left.compareTo(right);
    }

    private static Long parseLongOrNull(final String value)
    {
        try
        {
            return Long.parseLong(value);
        }
        catch (final NumberFormatException exception)
        {
            return null;
        }
    }

    private static Double parseDoubleOrNull(final String value)
    {
        try
        {
            return Double.parseDouble(value);
        }
        catch (final NumberFormatException exception)
        {
            return null;
        }
    }

    private static Long parseTimeMillisOrNull(final CharSequence value)
    {
        try
        {
            return Instant.parse(value).toEpochMilli();
        }
        catch (final Exception ignored)
        {
        }
        try
        {
            return LocalDateTime.parse(value).atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
        }
        catch (final Exception ignored)
        {
        }
        try
        {
            return LocalDate.parse(value).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
        }
        catch (final Exception ignored)
        {
            return null;
        }
    }

    private static <Item> List<Item> toList(final Iterable<Item> items)
    {
        final List<Item> result = new ArrayList<>();
        for (final Item item : items)
        {
            result.add(item);
        }
        return result;
    }

    private static int safeOffset(final int pageSize, final int currentPage)
    {
        final long offset = ((long) currentPage - 1L) * (long) pageSize;
        return offset > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, offset);
    }

    private static int safeOffset(final int pageSize, final int currentPage, final int size)
    {
        return Math.min(safeOffset(pageSize, currentPage), size);
    }

    private static int safeUpperBound(final int fromIndex, final int pageSize, final int size)
    {
        final long upperBound = (long) fromIndex + (long) pageSize;
        return (int) Math.min(upperBound, size);
    }

    record PreparedSearch(SearchCriteria criteria, PreparedSortOrder[] sortOrders)
    {
    }

    record PreparedSortOrder(String field, boolean ascending)
    {
    }

    record IndexedItem<Item>(Item item, Object key, Map<String, String> fields, String[] sortValues)
    {
    }
}
