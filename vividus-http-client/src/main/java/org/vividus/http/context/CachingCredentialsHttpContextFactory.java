/*
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vividus.http.context;

import org.apache.http.HttpHost;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.protocol.HttpContext;
import org.vividus.http.client.ClientBuilderUtils;

public class CachingCredentialsHttpContextFactory implements HttpContextFactory
{
    private final CredentialsProvider credentialsProvider;
    private final AuthCache authCache;

    public CachingCredentialsHttpContextFactory(String endpoint, String username, String password)
    {
        this.credentialsProvider = ClientBuilderUtils.createCredentialsProvider(username, password);
        this.authCache = new BasicAuthCache();
        authCache.put(HttpHost.create(endpoint), new BasicScheme());
    }

    @Override
    public HttpContext create()
    {
        HttpClientContext httpContext = HttpClientContext.create();
        httpContext.setAuthCache(authCache);
        httpContext.setCredentialsProvider(credentialsProvider);
        return httpContext;
    }
}
