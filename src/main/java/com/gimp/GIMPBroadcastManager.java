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

import com.gimp.locations.GIMPLocation;
import com.gimp.requests.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.HashMap;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

@Slf4j
public class GIMPBroadcastManager
{
	@Inject
	private GIMPHttpClient httpClient;

	@Inject
	private GIMPSocketClient socketClient;

	/**
	 * Get JSON string of the broadcast data.
	 *
	 * @param name     name of player
	 * @param location location of player
	 * @return JSON string of keys/values name, x, y, plane
	 */
	static String stringifyBroadcastData(String name, GIMPLocation location)
	{
		Map<String, Object> broadcastData = location.getLocation();
		String NAME_FIELD = "name";
		broadcastData.put(NAME_FIELD, name); // name, x, y, plane
		Gson gson = new Gson();
		return gson.toJson(broadcastData);
	}

	/**
	 * Parses JSON string of the ping data and adds to a Map.
	 *
	 * @param dataJson JSON string of ping data
	 * @return map: name => GIMPLocation
	 */
	static Map<String, GIMPLocation> parsePingData(String dataJson)
	{
		Gson gson = new Gson();
		Map<String, Map<String, Integer>> body = gson.fromJson(dataJson, new TypeToken<HashMap<String, Map<String, Integer>>>()
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

	/**
	 * Spoofs data returned from the ping request.
	 *
	 * @return map: name => GIMPLocation
	 */
	static Map<String, GIMPLocation> spoofPingData()
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
	 * Checks if socket client is connected.
	 *
	 * @return whether socket is connected
	 */
	public boolean isSocketConnected()
	{
		return socketClient.isConnected();
	}

	/**
	 * Connects socket client to the server.
	 */
	public void connectSocketClient()
	{
		socketClient.connect();
	}

	/**
	 * Gets broadcast client, using the socket client if it's
	 * connected and falling back on the HTTP client.
	 *
	 * @return a SocketClient or HTTPClient
	 */
	private GIMPRequestClient getRequestClient()
	{
		if (socketClient.isConnected())
		{
			return socketClient;
		}
		else
		{
			return httpClient;
		}
	}

	/**
	 * Sends broadcast request to the server via HTTP or socket.
	 *
	 * @param name     username of local player
	 * @param location GIMPLocation of local player
	 */
	public void broadcast(String name, GIMPLocation location)
	{
		try
		{
			GIMPRequestClient requestClient = getRequestClient();
			String dataJson = GIMPBroadcastManager.stringifyBroadcastData(name, location);
			requestClient.broadcast(dataJson);
		}
		catch (Exception e)
		{
			log.error(e.toString());
		}
	}

	/**
	 * Sends ping request to the server via HTTP or socket.
	 *
	 * @return map: name => GIMPLocation
	 */
	public Map<String, GIMPLocation> ping()
	{
		try
		{
			GIMPRequestClient requestClient = getRequestClient();
			String dataJson = requestClient.ping();
			return GIMPBroadcastManager.parsePingData(dataJson);
		}
		catch (Exception e)
		{
			log.error(e.toString());
			return new HashMap<>();
		}
	}
}
