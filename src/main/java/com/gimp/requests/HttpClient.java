/*
 * Copyright (c) 2021, David Vorona <davidavorona@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gimp.requests;

import com.gimp.GimPluginConfig;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.OkHttpClient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpClient extends RequestClient
{
	@Getter(AccessLevel.PACKAGE)
	public OkHttpClient client;

	public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

	public static final int OK = 200;

	public static final String EMPTY_BODY = "";

	public HttpClient(String namespace, OkHttpClient client, GimPluginConfig config)
	{
		this.config = config;
		this.namespace = namespace;
		this.client = client;
	}

	/**
	 * Makes an HTTP request with the given method to the URI at the client's
	 * base URL. Accepts on optional body argument for appropriate request methods.
	 *
	 * @param method HTTP method
	 * @param uri    URI path following base URL
	 * @param body   request body data
	 * @return future of response data in JSON
	 */
	private CompletableFuture<String> request(String method, String uri, RequestBody body)
	{
		CompletableFuture<String> result = new CompletableFuture<>();
		Request request = new Request.Builder()
			.url(getBaseUrl() + uri)
			.method(method, body)
			.build();
		client.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(@NonNull Call call, @NonNull IOException e)
			{
				log.error("Request failed: {}", String.valueOf(e));
			}

			@Override
			public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException
			{
				try (ResponseBody bodyJson = response.body())
				{
					if (!response.isSuccessful())
					{
						throw new IOException(response.code() + ": " + bodyJson);
					}
					if (bodyJson == null)
					{
						throw new RuntimeException("Response body is null: " + response);
					}
					result.complete(bodyJson.string());
				}
			}
		});
		return result;
	}

	/**
	 * Makes an HTTP GET request to the ping endpoint at the URL injected
	 * from the plugin config. A future of the JSON response body is returned.
	 *
	 * @return future of response data in JSON
	 */
	public CompletableFuture<String> ping()
	{
		return request("GET", "/ping/" + namespace, null);
	}

	/**
	 * Makes an HTTP POST request to the broadcast endpoint at the
	 * URL injected from the plugin config. The JSON data is sent
	 * in the request body. Times out after 5 seconds.
	 *
	 * @param dataJson request data in JSON
	 */
	public CompletableFuture<String> broadcast(String dataJson)
	{
		RequestBody body = RequestBody.create(JSON, dataJson);
		return request("POST", "/broadcast/" + namespace, body);
	}
}
