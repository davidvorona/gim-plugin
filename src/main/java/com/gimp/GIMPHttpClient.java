package com.gimp;

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

	public String ping() throws ExecutionException, InterruptedException, URISyntaxException
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
		String bodyJson = response.body();
		int OK = 200;
		if (response.statusCode() != OK)
		{
			log.error(bodyJson);
			return "";
		}
		return bodyJson;
	}

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
		int OK = 200;
		if (response.statusCode() != OK)
		{
			log.error(response.body());
		}
	}
}
