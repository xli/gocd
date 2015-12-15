package com.thoughtworks.go.agent;

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;

public class ResetableHttpClient implements HttpClient {
    public final static int TIMEOUT = 300;

    private HttpClient client;
    private SSLContext sslContext;
    private SystemEnvironment systemEnvironment;

    public ResetableHttpClient() {
         systemEnvironment = new SystemEnvironment();
    }

    public synchronized HttpClient get() {
        if (client == null) {
            client = this.create();
        }
        return client;
    }

    public synchronized void setSslContext(SSLContext context) {
        sslContext = context;
    }

    public synchronized void reset() {
        client = null;
    }

    private HttpClient create() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(TIMEOUT * 1000)
                .setSocketTimeout(TIMEOUT * 1000)
                .build();
        builder.setDefaultRequestConfig(config);
        if (this.sslContext != null) {
            builder.setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext,
                    new String[] { systemEnvironment.get(SystemEnvironment.GO_SSL_TRANSPORT_PROTOCOL_TO_BE_USED_BY_AGENT) },
                    null,
                    NoopHostnameVerifier.INSTANCE));
        }
        return builder.build();
    }

    @Override
    public HttpParams getParams() {
        return get().getParams();
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        return get().getConnectionManager();
    }

    @Override
    public HttpResponse execute(HttpUriRequest httpUriRequest) throws IOException, ClientProtocolException {
        return get().execute(httpUriRequest);
    }

    @Override
    public HttpResponse execute(HttpUriRequest httpUriRequest, HttpContext httpContext) throws IOException, ClientProtocolException {
        return get().execute(httpUriRequest, httpContext);
    }

    @Override
    public HttpResponse execute(HttpHost httpHost, HttpRequest httpRequest) throws IOException, ClientProtocolException {
        return get().execute(httpHost, httpRequest);
    }

    @Override
    public HttpResponse execute(HttpHost httpHost, HttpRequest httpRequest, HttpContext httpContext) throws IOException, ClientProtocolException {
        return get().execute(httpHost, httpRequest, httpContext);
    }

    @Override
    public <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return get().execute(httpUriRequest, responseHandler);
    }

    @Override
    public <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<? extends T> responseHandler, HttpContext httpContext) throws IOException, ClientProtocolException {
        return get().execute(httpUriRequest, responseHandler, httpContext);
    }

    @Override
    public <T> T execute(HttpHost httpHost, HttpRequest httpRequest, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return get().execute(httpHost, httpRequest, responseHandler);
    }

    @Override
    public <T> T execute(HttpHost httpHost, HttpRequest httpRequest, ResponseHandler<? extends T> responseHandler, HttpContext httpContext) throws IOException, ClientProtocolException {
        return get().execute(httpHost, httpRequest, responseHandler, httpContext);
    }
}
