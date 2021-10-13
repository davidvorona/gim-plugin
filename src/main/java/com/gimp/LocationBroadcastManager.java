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

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.HashMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@Slf4j
public class LocationBroadcastManager
{
	final HttpClient client;

	public HttpResponse<String> response;

	@Inject
	private GIMPConfig config;

	public LocationBroadcastManager()
	{
		client = HttpClient.newHttpClient();
	}

	public static Map<String, GIMPLocation> spoofPingData()
	{
		Map<String, GIMPLocation> data = new HashMap<>();
		// x = 2951, y = 3450: Doric's Anvil
		// x = 3220, y = 3219: Lumbridge Castle
		GIMPLocation gimpLocation1 = new GIMPLocation(2951, 3450, 0);
		GIMPLocation gimpLocation2 = new GIMPLocation(3220, 3219, 0);
		data.put("Manogram", gimpLocation1);
		data.put("Diregram", gimpLocation2);
		return data;
	}

	/**
	 * Get formatted JSON-ready object for broadcast POST.
	 *
	 * @param name     name of player
	 * @param location location of player
	 * @return map with keys/values name, x, y, plane
	 */
	public static Map<String, Object> getBroadcastData(String name, GIMPLocation location)
	{
		Map<String, Object> broadcastData = location.getLocation();
		String NAME_FIELD = "name";
		broadcastData.put(NAME_FIELD, name); // name, x, y, plane
		return broadcastData;
	}

	public boolean validateIpAndPort()
	{
		String ip = config.serverIp();
		String port = config.serverPort();
		if (ip == null || port == null)
		{
			return false;
		}
		// validate IP
		if (ip.contains("localhost"))
		{
			return true;
		}
		if (!ip.contains("."))
		{
			return false;
		}
		String[] terms = ip.split("\\.");
		for (String term : terms)
		{
			try
			{
				Integer.parseInt(term);
			}
			catch (NumberFormatException e)
			{
				return false;
			}
		}
		// validate port
		try
		{
			Integer.parseInt(port);
		}
		catch (NumberFormatException e)
		{
			return false;
		}
		return true;
	}

	private String getBaseUrl()
	{
		return "http://" + config.serverIp() + ":" + config.serverPort();
	}

	/**
	 * Pings server for location of fellow GIMPs
	 *
	 * @return map: name => location
	 */
	public Map<String, GIMPLocation> ping()
	{
		if (!validateIpAndPort())
		{
			log.warn("Invalid server port or IP, aborting request");
			return new HashMap<>();
		}
		try
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
				return new HashMap<>();
			}
			Gson gson = new Gson();
			Map<String, Map<String, Integer>> body = gson.fromJson(bodyJson, new TypeToken<HashMap<String, Map<String, Integer>>>()
			{
			}.getType());
			Map<String, GIMPLocation> data = new HashMap<>();
			for (String name : body.keySet())
			{
				Map<String, Integer> coordinates = body.get(name);
				GIMPLocation location = new GIMPLocation(
					coordinates.get("x"),
					coordinates.get("y"),
					coordinates.get("plane")
				);
				data.put(name, location);
			}
			return data;
		}
		catch (Exception e)
		{
			log.error(e.toString());
			return new HashMap<>();
		}
	}

	/**
	 * Broadcasts local GIMP's current location to server
	 *
	 * @param name     local player's name
	 * @param location map of player's location coordinates in x, y, plane
	 */
	public void broadcast(String name, GIMPLocation location)
	{
		if (!validateIpAndPort())
		{
			log.warn("Invalid server port or IP, aborting request");
			return;
		}
		Map<String, Object> body = LocationBroadcastManager.getBroadcastData(name, location);
		Gson gson = new Gson();
		String bodyJson = gson.toJson(body);
		try
		{
			HttpRequest request = HttpRequest.newBuilder()
				.uri(new URI(getBaseUrl() + "/broadcast"))
				.version(HttpClient.Version.HTTP_1_1)
				.timeout(Duration.of(5, ChronoUnit.SECONDS))
				.headers("Content-Type", "application/json;charset=UTF-8")
				.POST(HttpRequest.BodyPublishers.ofString(bodyJson))
				.build();
			response = client
				.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.get();
			int OK = 200;
			if (response.statusCode() != OK)
			{
				log.error(response.body());
			}
		}
		catch (Exception e)
		{
			log.error(e.toString());
		}
	}
}
