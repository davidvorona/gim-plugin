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
import java.util.concurrent.TimeUnit;
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

	public HttpClient(String namespace, GimPluginConfig config)
	{
		this.config = config;
		this.namespace = namespace;
		client = new OkHttpClient.Builder()
			.readTimeout(5000, TimeUnit.MILLISECONDS)
			.build();
	}

	/**
	 * Logs the HTTP response, specifying the status code if it's not 200.
	 *
	 * @param statusCode HTTP status code
	 * @param body       body of the HTTP response
	 */
	private void logHttpResponse(int statusCode, String body)
	{
		if (statusCode == OK)
		{
			log.debug(body);
		}
		else
		{
			log.error(statusCode + ": " + body);
		}
	}

	/**
	 * Makes an HTTP GET request to the ping endpoint at the URL injected
	 * from the plugin config. The JSON response body is returned. Times
	 * out after 5 seconds.
	 *
	 * @return response data in JSON
	 * @throws IOException if request fails
	 */
	public String ping() throws IOException
	{
		Request request = new Request.Builder()
			.url(getBaseUrl() + "/ping/" + namespace)
			.get()
			.build();
		Response response = client.newCall(request).execute();
		ResponseBody body = response.body();
		if (body != null)
		{
			String bodyJson = body.string();
			logHttpResponse(response.code(), bodyJson);
			return bodyJson;
		}
		return EMPTY_BODY;
	}

	/**
	 * Makes an HTTP POST request to the broadcast endpoint at the
	 * URL injected from the plugin config. The JSON data is sent
	 * in the request body. Times out after 5 seconds.
	 *
	 * @param dataJson request data in JSON
	 * @throws IOException if request fails
	 */
	public void broadcast(String dataJson) throws IOException
	{
		RequestBody body = RequestBody.create(JSON, dataJson);
		Request request = new Request.Builder()
			.url(getBaseUrl() + "/broadcast/" + namespace)
			.post(body)
			.build();
		Response response = client.newCall(request).execute();
		ResponseBody bodyJson = response.body();
		if (bodyJson != null)
		{
			logHttpResponse(response.code(), bodyJson.string());
		}
	}
}
