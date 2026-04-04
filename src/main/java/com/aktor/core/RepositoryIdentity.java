package com.aktor.core;

import com.aktor.core.exception.ConversionException;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

interface RepositoryIdentity
{
    String[] keyFieldNames(String defaultKeyFieldName);

    void bindKeyParameters(PreparedStatement statement, int startIndex, Object key) throws SQLException, ConversionException;

    boolean supportsNativeUpsert();

    static RepositoryIdentity of(final Class<?> type)
    {
        return RelationKeySpec.isRelationType(type) ? RelationRepositoryIdentity.INSTANCE : SingleColumnRepositoryIdentity.INSTANCE;
    }

    final class SingleColumnRepositoryIdentity
    implements RepositoryIdentity
    {
        private static final RepositoryIdentity INSTANCE = new SingleColumnRepositoryIdentity();

        private SingleColumnRepositoryIdentity()
        {
            super();
        }

        @Override
        public String[] keyFieldNames(final String defaultKeyFieldName)
        {
            return new String[] {Objects.requireNonNull(defaultKeyFieldName)};
        }

        @Override
        public void bindKeyParameters(final PreparedStatement statement, final int startIndex, final Object key)
        throws SQLException
        {
            statement.setObject(startIndex, key);
        }

        @Override
        public boolean supportsNativeUpsert()
        {
            return true;
        }
    }

    final class RelationRepositoryIdentity
    implements RepositoryIdentity
    {
        private static final RepositoryIdentity INSTANCE = new RelationRepositoryIdentity();

        private RelationRepositoryIdentity()
        {
            super();
        }

        @Override
        public String[] keyFieldNames(final String defaultKeyFieldName)
        {
            return RelationKeySpec.keyFields();
        }

        @Override
        public void bindKeyParameters(final PreparedStatement statement, final int startIndex, final Object key)
        throws SQLException, ConversionException
        {
            final Object[] parts = RelationKeySpec.decodeKey(key);
            int parameterIndex = startIndex;
            for (final Object part : parts)
            {
                statement.setObject(parameterIndex++, part);
            }
        }

        @Override
        public boolean supportsNativeUpsert()
        {
            return false;
        }
    }
}
