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
