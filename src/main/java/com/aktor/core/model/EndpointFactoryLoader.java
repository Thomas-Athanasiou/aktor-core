package com.aktor.web.http;

import com.aktor.core.model.Loader;

public interface EndpointFactoryLoader
extends Loader<EndpointFactory>
{
    String kind();
}
