package com.aktor.core.data;

import com.aktor.core.Data;
import com.aktor.core.util.CompositeKeyUtil;

import java.util.Objects;

public record Relation<Main, Foreign> (Main mainKey, Foreign foreignKey)
implements Data<String>
{
    public Relation
    {
        Objects.requireNonNull(mainKey);
        Objects.requireNonNull(foreignKey);
    }

    public String key()
    {
        return CompositeKeyUtil.encode(
            new String[]
            {
                mainKey.toString(),
                foreignKey.toString()
            }
        );
    }
}

