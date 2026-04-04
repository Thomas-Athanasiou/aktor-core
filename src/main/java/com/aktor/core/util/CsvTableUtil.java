package com.aktor.core.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CsvTableUtil
{
    private CsvTableUtil()
    {
        super();
    }

    public static CsvTable parse(final String csv)
    {
        final List<List<String>> rows = parseRows(csv);
        if (rows.isEmpty())
        {
            return new CsvTable(List.of(), List.of());
        }

        final List<String> headers = List.copyOf(rows.get(0));
        final List<Map<String, String>> items = new ArrayList<>(Math.max(0, rows.size() - 1));
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++)
        {
            final List<String> row = rows.get(rowIndex);
            if (isBlankRow(row))
            {
                continue;
            }
            final Map<String, String> values = new LinkedHashMap<>(headers.size());
            for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++)
            {
                final String value = columnIndex < row.size() ? row.get(columnIndex) : null;
                values.put(headers.get(columnIndex), value == null || value.isBlank() ? null : value);
            }
            items.add(values);
        }
        return new CsvTable(headers, items);
    }

    public static String serialize(final CsvTable csvTable)
    {
        Objects.requireNonNull(csvTable);
        return serialize(csvTable.headers(), csvTable.rows());
    }

    public static String serialize(
        final Iterable<String> headers,
        final Iterable<? extends Map<String, String>> rows
    )
    {
        final StringBuilder builder = new StringBuilder();
        appendRow(builder, headers, null);
        for (final Map<String, String> row : rows)
        {
            builder.append('\n');
            appendRow(builder, headers, row);
        }
        return builder.toString();
    }

    private static List<List<String>> parseRows(final String csv)
    {
        final List<List<String>> rows = new ArrayList<>();
        if (csv == null || csv.isBlank())
        {
            return rows;
        }

        final StringBuilder currentValue = new StringBuilder();
        List<String> currentRow = new ArrayList<>();
        boolean inQuotes = false;
        for (int index = 0; index < csv.length(); index++)
        {
            final char current = csv.charAt(index);
            if (current == '"')
            {
                if (inQuotes && index + 1 < csv.length() && csv.charAt(index + 1) == '"')
                {
                    currentValue.append('"');
                    index++;
                }
                else
                {
                    inQuotes = !inQuotes;
                }
            }
            else if (current == ',' && !inQuotes)
            {
                currentRow.add(currentValue.toString());
                currentValue.setLength(0);
            }
            else if ((current == '\n' || current == '\r') && !inQuotes)
            {
                currentRow.add(currentValue.toString());
                currentValue.setLength(0);
                rows.add(currentRow);
                currentRow = new ArrayList<>();
                if (current == '\r' && index + 1 < csv.length() && csv.charAt(index + 1) == '\n')
                {
                    index++;
                }
            }
            else
            {
                currentValue.append(current);
            }
        }

        if (!currentRow.isEmpty() || currentValue.length() > 0)
        {
            currentRow.add(currentValue.toString());
            rows.add(currentRow);
        }
        return rows;
    }

    private static boolean isBlankRow(final List<String> row)
    {
        for (final String value : row)
        {
            if (value != null && !value.isBlank())
            {
                return false;
            }
        }
        return true;
    }

    private static void appendRow(
        final StringBuilder builder,
        final Iterable<String> headers,
        final Map<String, String> row
    )
    {
        boolean first = true;
        for (final String header : headers)
        {
            if (!first)
            {
                builder.append(',');
            }
            final String value = row == null ? header : row.get(header);
            appendValue(builder, value);
            first = false;
        }
    }

    private static void appendValue(final StringBuilder builder, final String rawValue)
    {
        final String value = rawValue == null ? "" : rawValue;
        if (needsEscaping(value))
        {
            builder.append('"');
            for (int index = 0; index < value.length(); index++)
            {
                final char current = value.charAt(index);
                if (current == '"')
                {
                    builder.append("\"\"");
                }
                else
                {
                    builder.append(current);
                }
            }
            builder.append('"');
        }
        else
        {
            builder.append(value);
        }
    }

    private static boolean needsEscaping(final String value)
    {
        if (value.isEmpty())
        {
            return false;
        }
        return value.indexOf(',') >= 0
            || value.indexOf('"') >= 0
            || value.indexOf('\n') >= 0
            || value.indexOf('\r') >= 0
            || Character.isWhitespace(value.charAt(0))
            || Character.isWhitespace(value.charAt(value.length() - 1));
    }

    public record CsvTable(List<String> headers, List<Map<String, String>> rows)
    {
        public CsvTable
        {
            headers = List.copyOf(Objects.requireNonNull(headers));
            final List<Map<String, String>> safeRows = new ArrayList<>(Objects.requireNonNull(rows).size());
            for (final Map<String, String> row : rows)
            {
                safeRows.add(new LinkedHashMap<>(Objects.requireNonNull(row)));
            }
            rows = List.copyOf(safeRows);
        }
    }
}
