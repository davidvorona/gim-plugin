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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GIMPHttpClient extends GIMPRequestClient
{
	@Getter(AccessLevel.PACKAGE)
	public HttpClient client;

	public GIMPHttpClient()
	{
		client = HttpClient.newHttpClient();
	}

	/**
	 * Logs the HTTP response, specifying the status code if it's not 200.
	 *
	 * @param response HTTP response instance
	 */
	private void logHttpResponse(HttpResponse<String> response)
	{
		String bodyJson = response.body();
		int OK = 200;
		if (response.statusCode() == OK)
		{
			log.debug(bodyJson);
		}
		else
		{
			log.error(response.statusCode() + ": " + bodyJson);
		}
	}

	/**
	 * Makes an HTTP GET request to the ping endpoint at the URL injected
	 * from the plugin config. The JSON response body is returned. Times
	 * out after 5 seconds.
	 *
	 * @return response data in JSON
	 * @throws URISyntaxException   if base URL is invalid
	 * @throws ExecutionException   for unexpected HTTP request error
	 * @throws InterruptedException if HTTP request is interrupted
	 */
	public String ping() throws URISyntaxException, ExecutionException, InterruptedException
	{
		HttpRequest request = HttpRequest.newBuilder()
			.uri(new URI(getBaseUrl() + "/ping"))
			.version(HttpClient.Version.HTTP_1_1)
			.timeout(Duration.of(5, ChronoUnit.SECONDS))
			.headers("Content-Type", "application/json;charset=UTF-8")
			.GET()
			.build();
		HttpResponse<String> response = client
			.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.get();
		logHttpResponse(response);
		return response.body();
	}

	/**
	 * Makes an HTTP POST request to the broadcast endpoint at the
	 * URL injected from the plugin config. The JSON data is sent
	 * in the request body. Times out after 5 seconds.
	 *
	 * @param dataJson request data in JSON
	 * @throws URISyntaxException   if base URL is invalid
	 * @throws ExecutionException   for unexpected HTTP request error
	 * @throws InterruptedException if HTTP request is interrupted
	 */
	public void broadcast(String dataJson) throws URISyntaxException, ExecutionException, InterruptedException
	{

		HttpRequest request = HttpRequest.newBuilder()
			.uri(new URI(getBaseUrl() + "/broadcast"))
			.version(HttpClient.Version.HTTP_1_1)
			.timeout(Duration.of(5, ChronoUnit.SECONDS))
			.headers("Content-Type", "application/json;charset=UTF-8")
			.POST(HttpRequest.BodyPublishers.ofString(dataJson))
			.build();
		HttpResponse<String> response = client
			.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.get();
		logHttpResponse(response);
	}
}
