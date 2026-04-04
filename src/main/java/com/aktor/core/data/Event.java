package com.aktor.core.data;

import com.aktor.core.Data;

public interface Event<Key, Target>
extends Data<Key>
{
    Target target();

    long timestamp();
}
