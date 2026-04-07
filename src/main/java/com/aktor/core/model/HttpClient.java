package com.aktor.core.model;

import com.aktor.core.Model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

// TODO POLISH
public class HttpClient
implements Model
{
    public HttpClient()
    {
        this(LoggerConsole.INSTANCE);
    }

    public HttpClient(final Logger logger)
    {
        this.headers = new HashMap<>();
        this.cookies = new HashMap<>();
        this.responseStatus = 0;
        this.timeout = 300 * 1000;
        this.responseBody = null;
        this.responseHeaders = new HashMap<>();
        this.logger = logger == null ? LoggerConsole.INSTANCE : logger;
    }

    private final Map<String, String> headers;

    private final Logger logger;

    public final void addHeader(final String name, final String value)
    {
        headers.put(name, value);
    }

    public final void removeHeader(final String name)
    {
        headers.remove(name);
    }

    public final void clearHeaders()
    {
        headers.clear();
    }

    private final Map<String, String> responseHeaders;

    public final String getResponseHeader(final String name)
    {
        return responseHeaders.get(name);
    }

    protected int responseStatus;

    public final int getResponseStatus()
    {
        return responseStatus;
    }

    private int timeout;

    public final int getTimeout()
    {
        return timeout;
    }

    public final void setTimeout(final int nextTimeout)
    {
        timeout = nextTimeout;
    }

    private final Map<String, String> cookies;

    protected final Map<String, String> getCookies()
    {
        return cookies;
    }

    public final void addCookie(final String name, final String value)
    {
        getCookies().put(name, value);
    }

    public final void removeCookie(final String name)
    {
        getCookies().remove(name);
    }

    public final void clearCookies()
    {
        getCookies().clear();
    }

    public final void get(final String uri) throws IOException
    {
        makeRequest("GET", uri, null);
    }

    public final void post(final String uri, final String body) throws IOException
    {
        makeRequest("POST", uri, body);
    }

    public final void put(final String uri, final String body) throws IOException
    {
        makeRequest("PUT", uri, body);
    }

    public final void delete(final String uri, final String body) throws IOException
    {
        makeRequest("DELETE", uri, body);
    }

    private String responseBody;

    public final String getResponseBody()
    {
        return responseBody;
    }

    protected void makeRequest(final String method, final String uri, final String body) throws IOException
    {
        responseHeaders.clear();
        responseBody = null;
        responseStatus = 0;

        final HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(uri).openConnection();
        try
        {
            headers.forEach(httpURLConnection::setRequestProperty);
            @SuppressWarnings("NewApi") final String cookiesString = joinCookies();

            final boolean doOutput = body != null && !body.isEmpty();
            final int timeout = getTimeout();

            httpURLConnection.setRequestProperty("Cookie", cookiesString);
            httpURLConnection.setConnectTimeout(timeout);
            httpURLConnection.setReadTimeout(timeout);
            httpURLConnection.setInstanceFollowRedirects(true);
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(doOutput);
            httpURLConnection.setRequestMethod(method);
            httpURLConnection.connect();
            if (doOutput)
            {
                writeRequest(httpURLConnection, body);
            }
            responseBody = readResponse(httpURLConnection);
            responseStatus = httpURLConnection.getResponseCode();

            logger.debug("REQ URI:" + uri);
            logger.debug("REQ BODY:" + body);
            logger.debug("RES STATUS:" + responseStatus);
            logger.debug("RES BODY:" + responseBody);

            httpURLConnection.getHeaderFields().forEach(
                (key, value) -> {
                    if (key != null && value != null && !value.isEmpty())
                    {
                        responseHeaders.put(key, value.get(0));
                    }
                }
            );
        }
        finally
        {
            httpURLConnection.disconnect();
        }
    }

    private String joinCookies()
    {
        final StringBuilder builder = new StringBuilder();
        for (final Map.Entry<String, String> entry : cookies.entrySet())
        {
            builder.append(entry.getKey())
                .append('=')
                .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .append(';');
        }
        return builder.toString();
    }

    private String readResponse(final HttpURLConnection connection) throws IOException
    {
        final StringBuilder builder = new StringBuilder();
        final int status = connection.getResponseCode();
        try (final InputStream inputStream = status < HttpURLConnection.HTTP_BAD_REQUEST ? connection.getInputStream() : connection.getErrorStream())
        {
            if(inputStream != null)
            {
                try (final BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8)))
                {
                    String line;
                    boolean firstLine = true;
                    while ((line = reader.readLine()) != null)
                    {
                        if (!firstLine)
                        {
                            builder.append('\n');
                        }
                        builder.append(line);
                        firstLine = false;
                    }
                }
            }
        }
        return builder.toString();
    }

    private void writeRequest(final HttpURLConnection urlConnection, final String payload) throws IOException
    {
        try (final OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream(), StandardCharsets.UTF_8))
        {
            writer.write(payload);
        }
    }
}
