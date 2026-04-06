package com.aktor.core;

import com.aktor.core.exception.*;
import com.aktor.core.model.FieldResolver;
import com.aktor.core.model.RelationProviderResolver;
import com.aktor.core.model.TransactionParticipant;
import com.aktor.core.model.TransactionOrchestrator;
import com.aktor.core.util.CsvValuesUtil;
import com.aktor.core.util.SqlUtil;
import com.aktor.core.value.Filter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RepositorySql<Item extends java.lang.Record & Data<Key>, Key>
implements Repository<Item, Key>, TransactionParticipant
{
    private static final String LOGICAL_KEY_FIELD_NAME = "key";
    private final Connection connection;

    private final Class<? extends Data<?>> type;

    private final Converter<Item, DataRow> serializer;

    private final Converter<ResultSet, Item> mapper;

    private final Converter<SearchCriteria, String> searchSerializer;

    private final Converter<Item, String> updateParser;

    private final Converter<Item, String> insertParser;

    private final Converter<Key, String> getSerializer;

    private final Converter<Key, String> deleteSerializer;

    private final Converter<SearchCriteria, String> totalCountSerializer;

    private final Converter<Class<? extends Data<?>>, String> schemaSerializer;

    private final Converter<Item, String> upsertParser;

    private final RepositoryIdentity identity;

    private final String fixedGetSql;

    private final String fixedDeleteSql;

    private volatile boolean schemaChecked = false;

    private int transactionDepth = 0;

    private boolean ownsTransaction = false;

    private PreparedStatement cachedGetStatement;

    private PreparedStatement cachedDeleteStatement;

    private RepositorySql(
        final Connection connection,
        final Class<? extends Data<?>> type,
        final Converter<Item, DataRow> serializer,
        final Converter<ResultSet, Item> mapper,
        final Converter<Key, String> getSerializer,
        final Converter<SearchCriteria, String> searchSerializer,
        final Converter<Item, String> updateParser,
        final Converter<Item, String> insertParser,
        final Converter<Key, String> deleteSerializer,
        final Converter<SearchCriteria, String> totalCountSerializer,
        final Converter<Class<? extends Data<?>>, String> schemaSerializer,
        final Converter<Item, String> upsertParser,
        final String fixedGetSql,
        final String fixedDeleteSql
    )
    {
        super();
        this.connection = Objects.requireNonNull(connection);
        this.type = Objects.requireNonNull(type);
        this.serializer = Objects.requireNonNull(serializer);
        this.mapper = Objects.requireNonNull(mapper);
        this.getSerializer = Objects.requireNonNull(getSerializer);
        this.searchSerializer = Objects.requireNonNull(searchSerializer);
        this.updateParser = Objects.requireNonNull(updateParser);
        this.insertParser = Objects.requireNonNull(insertParser);
        this.deleteSerializer = Objects.requireNonNull(deleteSerializer);
        this.totalCountSerializer = Objects.requireNonNull(totalCountSerializer);
        this.schemaSerializer = Objects.requireNonNull(schemaSerializer);
        this.upsertParser = upsertParser;
        this.identity = RepositoryIdentity.of(type);
        this.fixedGetSql = fixedGetSql;
        this.fixedDeleteSql = fixedDeleteSql;
    }

    public RepositorySql(
        final Connection connection,
        final Class<Item> type,
        final String tableName,
        final String driverName
    )
    {
        this(connection, type, tableName, FieldResolver.mapped(type), driverName);
    }

    public RepositorySql(
        final Connection connection,
        final Class<Item> type,
        final String tableName,
        final RelationProviderResolver<Key> relationProviderResolver,
        final String driverName
    )
    {
        this(connection, type, tableName, FieldResolver.mapped(type), relationProviderResolver, driverName);
    }

    public RepositorySql(
        final Connection connection,
        final Class<Item> type,
        final String tableName,
        final FieldResolver fieldResolver,
        final String driverName
    )
    {
        this(connection, type, tableName, fieldResolver, new RelationProviderResolver<>(), driverName);
    }

    public RepositorySql(
        final Connection connection,
        final Class<Item> type,
        final String tableName,
        final FieldResolver fieldResolver,
        final RelationProviderResolver<Key> relationProviderResolver,
        final String driverName
    )
    {
        this(
            connection,
            type,
            createResultSetParser(type, fieldResolver, relationProviderResolver),
            createSqlParsers(
                type,
                tableName,
                driverName,
                fieldResolver
            )
        );
    }

    private RepositorySql(
        final Connection connection,
        final Class<? extends Data<?>> type,
        final Converter<ResultSet, Item> mapper,
        final SqlParsers<Item, Key> sqlParsers
    )
    {
        this(
            connection,
            type,
            sqlParsers.serializer(),
            mapper,
            sqlParsers.getSerializer(),
            sqlParsers.searchSerializer(),
            sqlParsers.updateParser(),
            sqlParsers.insertParser(),
            sqlParsers.deleteSerializer(),
            sqlParsers.totalCountSerializer(),
            sqlParsers.schemaSerializer(),
            sqlParsers.upsertParser(),
            sqlParsers.fixedGetSql(),
            sqlParsers.fixedDeleteSql()
        );
    }

    private static <Item extends java.lang.Record & Data<Key>, Key> SqlParsers<Item, Key> createSqlParsers(
        final Class<? extends Data<?>> type,
        final String tableName,
        final String driverName
    )
    {
        return new SqlParsersFactory<Item, Key>(type, tableName, driverName, FieldResolver.mapped(type)).create();
    }

    private static <Item extends Record & Data<Key>, Key> SqlParsers<Item, Key> createSqlParsers(
        final Class<? extends Data<?>> type,
        final String tableName,
        final String driverName,
        final FieldResolver fieldResolver
    )
    {
        return new SqlParsersFactory<Item, Key>(type, tableName, driverName, fieldResolver).create();
    }

    private static <Item extends Record & Data<Key>, Key> Converter<ResultSet, Item> createResultSetParser(
        final Class<Item> type,
        final FieldResolver fieldResolver,
        final RelationProviderResolver<Key> relationProviderResolver
    )
    {
        final Mapper<Item, Key> mapper = new Mapper<>(fieldResolver, relationProviderResolver, type);
        return new DataParserResultSet<>(mapper);
    }

    record SqlParsers<Item extends Record & Data<Key>, Key>(
        Converter<Item, DataRow> serializer,
        Converter<Key, String> getSerializer,
        Converter<SearchCriteria, String> searchSerializer,
        Converter<Item, String> updateParser,
        Converter<Item, String> insertParser,
        Converter<Key, String> deleteSerializer,
        Converter<SearchCriteria, String> totalCountSerializer,
        Converter<Class<? extends Data<?>>, String> schemaSerializer,
        Converter<Item, String> upsertParser,
        String fixedGetSql,
        String fixedDeleteSql
    )
    {

    }

    private static final class SqlParsersFactory<Item extends Record & Data<Key>, Key>
    {
        private final String tableName;

        private final String driverName;

        private final DataRowMapper<Item, Key> dataRowMapper;

        private final RepositoryIdentity identity;

        private final com.aktor.core.model.SqlDialect sqlDialect;

        private final FieldResolver fieldResolver;

        private SqlParsersFactory(
            final Class<? extends Data<?>> type,
            final String tableName,
            final String driverName,
            final FieldResolver fieldResolver
        )
        {
            super();
            this.tableName = Objects.requireNonNull(tableName);
            this.driverName = Objects.requireNonNull(driverName);
            this.dataRowMapper = new DataRowMapper<>(fieldResolver);
            final Class<? extends Data<?>> safeType = Objects.requireNonNull(type);
            this.identity = RepositoryIdentity.of(safeType);
            this.sqlDialect = SqlUtil.ofDialect(driverName);
            this.fieldResolver = Objects.requireNonNull(fieldResolver);
        }

        private SqlParsers<Item, Key> create()
        {
            final String[] keyFieldNames = identity.keyFieldNames(fieldResolver.resolve(LOGICAL_KEY_FIELD_NAME));
            return new SqlParsers<>(
                dataRowMapper,
                new KeySqlSelectParser<>(tableName, keyFieldNames, sqlDialect.quoteStart(), sqlDialect.quoteEnd()),
                new SearchCriteriaSqlSearchParser(
                    tableName,
                    sqlDialect.quoteStart(),
                    sqlDialect.quoteEnd(),
                    sqlDialect,
                    fieldResolver
                ),
                new DataRowSqlUpdateParser<>(tableName, keyFieldNames, sqlDialect.quoteStart(), sqlDialect.quoteEnd(), dataRowMapper),
                SqlUtil.ofDataRowInsertParser(tableName, dataRowMapper, driverName),
                new KeySqlDeleteParser<>(tableName, keyFieldNames, sqlDialect.quoteStart(), sqlDialect.quoteEnd()),
                new SearchCriteriaSqlTotalCountParser(
                    tableName,
                    sqlDialect.quoteStart(),
                    sqlDialect.quoteEnd(),
                    fieldResolver
                ),
                SqlUtil.ofClassSchemaParser(tableName, driverName, fieldResolver),
                identity.supportsNativeUpsert()
                    ? SqlUtil.ofDataRowUpsertParser(tableName, dataRowMapper, driverName)
                    : null,
                SqlParserUtil.selectByKeySql(tableName, keyFieldNames, sqlDialect.quoteStart(), sqlDialect.quoteEnd()),
                SqlParserUtil.deleteByKeySql(tableName, keyFieldNames, sqlDialect.quoteStart(), sqlDialect.quoteEnd())
            );
        }
    }

    @Override
    public Item get(final Key key) throws GetException
    {
        final Item item;

        try
        {
            ensureSchema();
        }
        catch (final Exception exception)
        {
            throw new GetException(exception);
        }

        if (fixedGetSql != null)
        {
            try
            {
                synchronized (this)
                {
                    final PreparedStatement statement = getCachedGetStatement();
                    statement.clearParameters();
                    bindKeyParameters(statement, key);
                    try (final ResultSet resultSet = statement.executeQuery())
                    {
                        if (resultSet.next())
                        {
                            item = mapper.convert(resultSet);
                        }
                        else
                        {
                            throw new GetException(
                                "The item with key identity of '" + key + "' that was requested doesn't exist, verify the item and try again"
                            );
                        }
                    }
                }
            }
            catch (final SQLException | ConversionException exception)
            {
                throw new GetException(exception);
            }
        }
        else
        {
            try (final PreparedStatement statement = connection.prepareStatement(getSerializer.convert(key)))
            {
                bindKeyParameters(statement, key);
                try (final ResultSet resultSet = statement.executeQuery())
                {
                    if (resultSet.next())
                    {
                        item = mapper.convert(resultSet);
                    }
                    else
                    {
                        throw new GetException(
                            "The item with key identity of '" + key + "' that was requested doesn't exist, verify the item and try again"
                        );
                    }
                }
            }
            catch (final SQLException | ConversionException exception)
            {
                throw new GetException(exception);
            }
        }
        return item;
    }

    @Override
    public SearchResult<Item> search(final SearchCriteria searchCriteria) throws SearchException
    {
        if (searchCriteria == null)
        {
            throw new SearchException("Search criteria must not be null");
        }
        final int totalCount = searchTotalCount(searchCriteria);
        final List<Item> results = new ArrayList<>(Math.min(totalCount, searchCriteria.pageSize()));
        if(totalCount > 0)
        {
            try (final PreparedStatement statement = connection.prepareStatement(searchSerializer.convert(searchCriteria)))
            {
                RepositorySql.bindSearchParameters(statement, searchCriteria);

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
        return new SearchResult<>(results, searchCriteria, totalCount);
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
        executeWrite(item, upsertParser, null);
    }

    private int update(final Item item) throws SaveException
    {
        return executeWrite(item, updateParser, identity::bindKeyParameters);
    }

    private void insert(final Item item) throws SaveException
    {
        executeWrite(item, insertParser, null);
    }

    @Override
    public synchronized void delete(final Item item) throws DeleteException
    {
        try
        {
            executeWithinTransaction(
                () -> {
                    if (fixedDeleteSql != null)
                    {
                        final PreparedStatement statement = getCachedDeleteStatement();
                        statement.clearParameters();
                        ensureSchema();
                        bindKeyParameters(statement, item.key());
                        statement.executeUpdate();
                    }
                    else
                    {
                        try (final PreparedStatement statement = connection.prepareStatement(deleteSerializer.convert(item.key())))
                        {
                            ensureSchema();
                            bindKeyParameters(statement, item.key());
                            statement.executeUpdate();
                        }
                    }
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
        final boolean canCommit = transactionDepth > 0;
        if (canCommit)
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
        final boolean canRollback = transactionDepth > 0;
        if (canRollback)
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

    private int searchTotalCount(final SearchCriteria searchCriteria) throws SearchException
    {
        final int totalCount;

        try
        {
            ensureSchema();
        }
        catch (final ConversionException | SQLException exception)
        {
            throw new SearchException(exception);
        }

        try (final PreparedStatement statement = connection.prepareStatement(totalCountSerializer.convert(searchCriteria)))
        {
            RepositorySql.bindSearchParameters(statement, searchCriteria);

            try (final ResultSet resultSet = statement.executeQuery())
            {
                if (resultSet.next())
                {
                    final long value = resultSet.getLong(1);
                    if (resultSet.wasNull())
                    {
                        throw new SearchException("Total count query returned NULL");
                    }
                    else if (value < 0L || value > Integer.MAX_VALUE)
                    {
                        throw new SearchException("Total count out of int range: " + value);
                    }
                    totalCount = (int) value;
                }
                else
                {
                    throw new SearchException("No results in total count query");
                }
            }
        }
        catch (final SQLException | ConversionException exception)
        {
            throw new SearchException(exception);
        }
        return totalCount;
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

    private int executeWrite(
        final Item item,
        final Converter<Item, String> sqlParser,
        final KeyParameterBinder<Key> keyParameterBinder
    ) throws SaveException
    {
        try
        {
            ensureSchema();
            final Value[] values = serializer.convert(item).values();
            try (final PreparedStatement statement = connection.prepareStatement(sqlParser.convert(item)))
            {
                bindValueParameters(statement, values);
                if (keyParameterBinder != null)
                {
                    bindKeyParameters(statement, values.length + 1, item.key(), keyParameterBinder);
                }
                return statement.executeUpdate();
            }
        }
        catch (final ConversionException | SQLException exception)
        {
            throw new SaveException(exception);
        }
    }

    private static void bindValueParameters(final PreparedStatement statement, final Value[] values) throws SQLException
    {
        for (int index = 0; index < values.length; index++)
        {
            statement.setObject(index + 1, values[index].value());
        }
    }

    private void bindKeyParameters(final PreparedStatement statement, final Key key)
    throws SQLException, ConversionException
    {
        bindKeyParameters(statement, 1, key, identity::bindKeyParameters);
    }

    private void bindKeyParameters(
        final PreparedStatement statement,
        final int parameterIndex,
        final Key key,
        final KeyParameterBinder<Key> keyParameterBinder
    ) throws SQLException, ConversionException
    {
        keyParameterBinder.bind(statement, parameterIndex, key);
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

    @FunctionalInterface
    private interface KeyParameterBinder<Key>
    {
        void bind(PreparedStatement statement, int parameterIndex, Key key) throws SQLException, ConversionException;
    }

    @FunctionalInterface
    private interface SqlOperation
    {
        void run() throws Exception;
    }
}
