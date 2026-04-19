package com.aktor.web.http;

import com.aktor.core.exception.ConversionException;

public interface Router
{
    Response route(Route route, Request request) throws ConversionException;

    Response route(Request request) throws ConversionException;
}
