package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.exception.DeleteException;
import com.aktor.core.exception.GetException;
import com.aktor.core.exception.SaveException;
import com.aktor.core.exception.SearchException;
import com.aktor.core.model.FieldResolver;
import com.aktor.core.model.RelationProviderResolver;
import com.aktor.core.model.CollectionProcessor;
import com.aktor.core.model.SearchCriteriaCondition;
import com.aktor.core.model.SqlDialect;
import com.aktor.core.model.SqlDialectResolver;
import com.aktor.core.model.TransactionParticipant;
import com.aktor.core.model.TransactionOrchestrator;
import com.aktor.core.util.CsvValuesUtil;
import com.aktor.core.value.Filter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RepositorySql<Item extends Data<Key>, Key>
extends SearchExecutorRelational<Item, Key>
implements Repository<Item, Key>, TransactionParticipant
{
    private static final String LOGICAL_KEY_FIELD_NAME = "key";

    private final Connection connection;
    private final Class<? extends Data<?>> type;
    private final Converter<Item, Row> serializer;
    private final Converter<ResultSet, Item> mapper;
    private final Converter<SearchCriteria, String> searchSerializer;
    private final Converter<Item, String> updateParser;
    private final Converter<Item, String> insertParser;
    private final Converter<SearchCriteria, String> totalCountSerializer;
    private final Converter<Class<? extends Data<?>>, String> schemaSerializer;
    private final Converter<Item, String> upsertParser;
    private final String fixedGetSql;
    private final String fixedDeleteSql;

    private volatile boolean schemaChecked = false;

    private int transactionDepth = 0;
    private boolean ownsTransaction = false;
    private PreparedStatement cachedGetStatement = null;
    private PreparedStatement cachedDeleteStatement = null;

    private RepositorySql(
        final Connection connection,
        final Class<? extends Data<?>> type,
        final Converter<Item, Row> serializer,
        final Converter<ResultSet, Item> mapper,
        final Converter<SearchCriteria, String> searchSerializer,
        final Converter<Item, String> updateParser,
        final Converter<Item, String> insertParser,
        final Converter<SearchCriteria, String> totalCountSerializer,
        final Converter<Class<? extends Data<?>>, String> schemaSerializer,
        final Converter<Item, String> upsertParser,
        final String fixedGetSql,
        final String fixedDeleteSql
    )
    {
        super(new CollectionProcessor<>(new SearchCriteriaCondition(), serializer));
        this.connection = Objects.requireNonNull(connection);
        this.type = Objects.requireNonNull(type);
        this.serializer = Objects.requireNonNull(serializer);
        this.mapper = Objects.requireNonNull(mapper);
        this.searchSerializer = Objects.requireNonNull(searchSerializer);
        this.updateParser = Objects.requireNonNull(updateParser);
        this.insertParser = Objects.requireNonNull(insertParser);
        this.totalCountSerializer = Objects.requireNonNull(totalCountSerializer);
        this.schemaSerializer = Objects.requireNonNull(schemaSerializer);
        this.upsertParser = upsertParser;
        this.fixedGetSql = Objects.requireNonNull(fixedGetSql);
        this.fixedDeleteSql = Objects.requireNonNull(fixedDeleteSql);
    }

    public static <Item extends Data<Key>, Key> Repository<Item, Key> of(
        final Connection connection,
        final Class<Item> type,
        final String table,
        final String driver
    )
    {
        return of(connection, type, table, driver, FieldResolver.mapped(type), new RelationProviderResolver<>());
    }

    public static <Item extends Data<Key>, Key> Repository<Item, Key> of(
        final Connection connection,
        final Class<Item> type,
        final String table,
        final String driver,
        final RelationProviderResolver<Key> relationProviderResolver
    )
    {
        return of(connection, type, table, driver, FieldResolver.mapped(type), relationProviderResolver);
    }

    public static <Item extends Data<Key>, Key> Repository<Item, Key> of(
        final Connection connection,
        final Class<Item> type,
        final String table,
        final String driver,
        final FieldResolver fieldResolver
    )
    {
        return of(connection, type, table, driver, fieldResolver, new RelationProviderResolver<>());
    }

    public static <Item extends Data<Key>, Key> Repository<Item, Key> of(
        final Connection connection,
        final Class<Item> type,
        final String table,
        final String driver,
        final FieldResolver fieldResolver,
        final RelationProviderResolver<Key> relationProviderResolver
    )
    {
        final FieldResolver safeFieldResolver = Objects.requireNonNull(fieldResolver);
        final SqlDialect sqlDialect = SqlDialectResolver.of(driver);
        final DataRowMapper<Item, Key> dataRowMapper = new DataRowMapper<>(safeFieldResolver);
        final String keyFieldName = Objects.requireNonNull(safeFieldResolver.resolve(LOGICAL_KEY_FIELD_NAME));
        return new RepositorySql<>(
            connection,
            type,
            dataRowMapper,
            createResultSetParser(type, safeFieldResolver, relationProviderResolver),
            SearchCriteriaSqlSearchParser.of(table, driver, safeFieldResolver),
            SqlUpdateParser.of(table, driver, dataRowMapper, safeFieldResolver),
            SqlInsertParser.of(table, driver, dataRowMapper),
            SearchCriteriaSqlTotalCountParser.of(table, driver, safeFieldResolver),
            ClassSqlSchemaParser.of(table, driver, safeFieldResolver),
            SqlUpsertParser.of(table, driver, dataRowMapper, safeFieldResolver),
            SqlParserUtil.selectByKeySql(table, new String[]{keyFieldName}, sqlDialect.quoteStart(), sqlDialect.quoteEnd()),
            SqlParserUtil.deleteByKeySql(table, new String[]{keyFieldName}, sqlDialect.quoteStart(), sqlDialect.quoteEnd())
        );
    }

    @Override
    public Item get(final Key key) throws GetException
    {
        try
        {
            ensureSchema();
            synchronized (this)
            {
                final PreparedStatement statement = getCachedGetStatement();
                statement.clearParameters();
                bindKeyParameter(statement, key);
                try (final ResultSet resultSet = statement.executeQuery())
                {
                    if (resultSet.next())
                    {
                        return mapper.convert(resultSet);
                    }
                }
            }
            throw new GetException(
                "The item with key identity of '" + key + "' that was requested doesn't exist, verify the item and try again"
            );
        }
        catch (final Exception exception)
        {
            if (exception instanceof final GetException getException)
            {
                throw getException;
            }
            throw new GetException(exception);
        }
    }

    @Override
    protected SearchSource<Item, Key> searchSource(final SearchCriteria searchCriteria) throws SearchException
    {
        return () -> loadCandidates(searchCriteria);
    }

    @Override
    protected SearchResult<Item> searchNative(final SearchCriteria searchCriteria) throws SearchException
    {
        return new SearchResult<>(
            loadCandidates(searchCriteria),
            searchCriteria,
            searchTotalCount(searchCriteria)
        );
    }

    private List<Item> loadCandidates(final SearchCriteria searchCriteria) throws SearchException
    {
        final SearchCriteria safeSearchCriteria = Objects.requireNonNull(searchCriteria);
        final List<Item> results = new ArrayList<>();
        if (safeSearchCriteria.pageSize() > 0)
        {
            try (final PreparedStatement statement = connection.prepareStatement(searchSerializer.convert(safeSearchCriteria)))
            {
                bindSearchParameters(statement, safeSearchCriteria);
                try (final ResultSet resultSet = statement.executeQuery())
                {
                    while (resultSet.next())
                    {
                        results.add(mapper.convert(resultSet));
                    }
                }
            }
            catch (final SQLException | ConversionException exception)
            {
                throw new SearchException(exception);
            }
        }
        return results;
    }

    @Override
    public synchronized void save(final Item item) throws SaveException
    {
        try
        {
            executeWithinTransaction(
                () -> {
                    if (upsertParser == null)
                    {
                        saveWithFallback(item);
                    }
                    else
                    {
                        upsert(item);
                    }
                }
            );
        }
        catch (final Exception exception)
        {
            if (exception instanceof final SaveException saveException)
            {
                throw saveException;
            }
            throw new SaveException(exception);
        }
    }

    @Override
    public synchronized void delete(final Item item) throws DeleteException
    {
        try
        {
            executeWithinTransaction(
                () -> {
                    final PreparedStatement statement = getCachedDeleteStatement();
                    statement.clearParameters();
                    ensureSchema();
                    bindKeyParameter(statement, item.key());
                    statement.executeUpdate();
                }
            );
        }
        catch (final Exception exception)
        {
            if (exception instanceof final DeleteException deleteException)
            {
                throw deleteException;
            }
            throw new DeleteException(exception);
        }
    }

    @Override
    public synchronized void beginTransaction() throws SQLException
    {
        if (transactionDepth == 0)
        {
            ownsTransaction = connection.getAutoCommit();
            if (ownsTransaction)
            {
                connection.setAutoCommit(false);
            }
        }
        transactionDepth++;
    }

    @Override
    public synchronized void commitTransaction() throws SQLException
    {
        if (transactionDepth > 0)
        {
            transactionDepth--;
            if (transactionDepth == 0 && ownsTransaction)
            {
                try
                {
                    connection.commit();
                }
                finally
                {
                    connection.setAutoCommit(true);
                    ownsTransaction = false;
                }
            }
        }
    }

    @Override
    public synchronized void rollbackTransaction() throws SQLException
    {
        if (transactionDepth > 0)
        {
            transactionDepth = 0;
            if (ownsTransaction)
            {
                try
                {
                    connection.rollback();
                }
                finally
                {
                    connection.setAutoCommit(true);
                    ownsTransaction = false;
                }
            }
        }
    }

    private void saveWithFallback(final Item item) throws SaveException
    {
        if (update(item) < 1)
        {
            try
            {
                insert(item);
            }
            catch (final SaveException exception)
            {
                if (update(item) < 1)
                {
                    throw exception;
                }
            }
        }
    }

    private void upsert(final Item item) throws SaveException
    {
        executeWrite(item, upsertParser, false);
    }

    private int update(final Item item) throws SaveException
    {
        return executeWrite(item, updateParser, true);
    }

    private void insert(final Item item) throws SaveException
    {
        executeWrite(item, insertParser, false);
    }

    private int searchTotalCount(final SearchCriteria searchCriteria) throws SearchException
    {
        try
        {
            ensureSchema();
            try (final PreparedStatement statement = connection.prepareStatement(totalCountSerializer.convert(searchCriteria)))
            {
                bindSearchParameters(statement, searchCriteria);
                try (final ResultSet resultSet = statement.executeQuery())
                {
                    if (resultSet.next())
                    {
                        final long value = resultSet.getLong(1);
                        if (resultSet.wasNull())
                        {
                            throw new SearchException("Total count query returned NULL");
                        }
                        if (value < 0L || value > Integer.MAX_VALUE)
                        {
                            throw new SearchException("Total count out of int range: " + value);
                        }
                        return (int) value;
                    }
                }
            }
            throw new SearchException("No results in total count query");
        }
        catch (final Exception exception)
        {
            if (exception instanceof final SearchException searchException)
            {
                throw searchException;
            }
            throw new SearchException(exception);
        }
    }

    private int executeWrite(final Item item, final Converter<Item, String> parser, final boolean bind) throws SaveException
    {
        try
        {
            ensureSchema();
            final Value[] values = serializer.convert(item).values();
            try (final PreparedStatement statement = connection.prepareStatement(parser.convert(item)))
            {
                bindValueParameters(statement, values);
                if (bind)
                {
                    bindKeyParameter(statement, values.length + 1, item.key());
                }
                return statement.executeUpdate();
            }
        }
        catch (final ConversionException | SQLException exception)
        {
            throw new SaveException(exception);
        }
    }

    private static void bindSearchParameters(final PreparedStatement statement, final SearchCriteria searchCriteria)
    throws SQLException
    {
        int parameterIndex = 1;
        for (final FilterGroup filterGroup : searchCriteria.filterGroups())
        {
            for (final Filter filter : filterGroup.filters())
            {
                switch (filter.conditionType())
                {
                    case IS_NULL:
                    case IS_NOT_NULL:
                        break;
                    case IN:
                    case NOT_IN:
                        for (final String value : CsvValuesUtil.split(filter.value()))
                        {
                            statement.setObject(parameterIndex++, value);
                        }
                        break;
                    default:
                        statement.setObject(parameterIndex++, filter.value());
                        break;
                }
            }
        }
    }

    private static void bindValueParameters(final PreparedStatement statement, final Value[] values) throws SQLException
    {
        for (int index = 0; index < values.length; index++)
        {
            statement.setObject(index + 1, values[index].value());
        }
    }

    private void bindKeyParameter(final PreparedStatement statement, final Key key)
    throws SQLException
    {
        bindKeyParameter(statement, 1, key);
    }

    private void bindKeyParameter(final PreparedStatement statement, final int index, final Key key) throws SQLException
    {
        statement.setObject(index, key);
    }

    private void executeWithinTransaction(final SqlOperation operation) throws Exception
    {
        TransactionOrchestrator.execute(List.of(this), operation::run);
    }

    private void ensureSchema() throws SQLException, ConversionException
    {
        if (!schemaChecked)
        {
            synchronized (this)
            {
                if (!schemaChecked)
                {
                    try (final PreparedStatement statement = connection.prepareStatement(schemaSerializer.convert(type)))
                    {
                        statement.executeUpdate();
                        schemaChecked = true;
                    }
                }
            }
        }
    }

    private PreparedStatement getCachedGetStatement() throws SQLException
    {
        if (cachedGetStatement == null || cachedGetStatement.isClosed())
        {
            cachedGetStatement = connection.prepareStatement(fixedGetSql);
        }
        return cachedGetStatement;
    }

    private PreparedStatement getCachedDeleteStatement() throws SQLException
    {
        if (cachedDeleteStatement == null || cachedDeleteStatement.isClosed())
        {
            cachedDeleteStatement = connection.prepareStatement(fixedDeleteSql);
        }
        return cachedDeleteStatement;
    }

    private static <Item extends Data<Key>, Key> Converter<ResultSet, Item> createResultSetParser(
        final Class<Item> type,
        final FieldResolver fieldResolver,
        final RelationProviderResolver<Key> relationProviderResolver
    )
    {
        return new DataParserResultSet<>(new Mapper<Item, Key>(fieldResolver, relationProviderResolver, type));
    }

    @FunctionalInterface
    private interface SqlOperation
    {
        void run() throws Exception;
    }
}
