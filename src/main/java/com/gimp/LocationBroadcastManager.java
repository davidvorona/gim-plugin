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
package com.gimp;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import com.google.gson.Gson;

@Slf4j
public class LocationBroadcastManager
{
	final HttpClient client;

	public HttpResponse<String> response;

	public LocationBroadcastManager()
	{
		client = HttpClient.newHttpClient();
	}

	public static Map<String, Location> spoofPingData()
	{
		Map<String, Location> data = new HashMap<>();
		// x = 2951, y = 3450: Doric's Anvil
		Location gimpLocation = new Location(2951, 3450, 0);
		data.put("Manogram", gimpLocation);
		return data;
	}

	/**
	 * Pings server for location of fellow GIMPs
	 *
	 * @return String
	 */
	public Map<String, Location> ping() throws ExecutionException, InterruptedException, URISyntaxException
	{
		HttpRequest request = HttpRequest.newBuilder()
			.uri(new URI("https://postman-echo.com/get"))
			.timeout(Duration.of(5, ChronoUnit.SECONDS))
			.GET()
			.build();
		HttpResponse<String> response = client
			.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.get();
		String bodyJson = response.body();
//        Gson gson = new Gson();
//        Map<String, Object> body = gson.fromJson(bodyJson, HashMap.class);
//        GIMPLocation.sanitizeLocationData(body);
		log.info(bodyJson);
		// spoof response
		return LocationBroadcastManager.spoofPingData();
	}

	/**
	 * Broadcasts local GIMP's current location to server
	 *
	 * @param location map of player's location coordinates in x, y, plane
	 */
	public void broadcast(GIMPLocation location) throws URISyntaxException, ExecutionException, InterruptedException
	{
		Map<String, Object> body = location.getLocation();
		Gson gson = new Gson();
		String bodyJson = gson.toJson(body);
		HttpRequest request = HttpRequest.newBuilder()
			.uri(new URI("https://postman-echo.com/post"))
			.timeout(Duration.of(5, ChronoUnit.SECONDS))
			.POST(HttpRequest.BodyPublishers.ofString(bodyJson))
			.build();
		response = client
			.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.get();
		log.info(response.body());
	}
}
