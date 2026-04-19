package com.aktor.web.http;

@FunctionalInterface
public interface EndpointResolver
{
    String resolve(Request request);
}
