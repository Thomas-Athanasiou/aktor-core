package com.aktor.web.http;

import com.aktor.core.model.Loader;

public interface RouterFactoryLoader
extends Loader<RouterFactory>
{
    String kind();
}
