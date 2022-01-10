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

import com.gimp.gimps.GimPlayer;
import com.gimp.requests.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import java.lang.reflect.Type;
import java.util.HashMap;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

@Slf4j
public class GimBroadcastManager
{
	public static final Type pingDataTypeForJson = new TypeToken<Map<String, GimPlayer>>()
	{
	}.getType();

	@Inject
	private HttpClient httpClient;

	@Inject
	private SocketClient socketClient;

	/**
	 * Parses JSON string of the ping data and adds to a Map.
	 *
	 * @param dataJson JSON string of ping data
	 * @return map: name => GIMPLocation
	 */
	static Map<String, GimPlayer> parsePingData(String dataJson)
	{
		Gson gson = new Gson();
		return gson.fromJson(dataJson, pingDataTypeForJson);
	}

	/**
	 * Parses JSON string of the broadcast data and maps to a GimPlayer instance.
	 *
	 * @param dataJson JSON string of broadcast data
	 * @return GimPlayer
	 */
	static GimPlayer parseBroadcastData(String dataJson)
	{
		Gson gson = new Gson();
		return gson.fromJson(dataJson, GimPlayer.class);
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
	 * Disconnects socket client from the server.
	 */
	public void disconnectSocketClient()
	{
		socketClient.disconnect();
	}

	/**
	 * Gets broadcast client, using the socket client if it's
	 * connected and falling back on the HTTP client.
	 *
	 * @return a SocketClient or HTTPClient
	 */
	private RequestClient getRequestClient()
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

	public void listen(Emitter.Listener handleBroadcast)
	{
		Socket client = socketClient.getClient();
		client.on("broadcast", handleBroadcast);
	}

	/**
	 * Sends broadcast request to the server via HTTP or socket.
	 */
	public void broadcast(Map<String, Object> data)
	{
		try
		{
			RequestClient requestClient = getRequestClient();
			Gson gson = new Gson();
			String dataJson = gson.toJson(data);
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
	 * @return map: name => GimPlayer
	 */
	public Map<String, GimPlayer> ping()
	{
		try
		{
			RequestClient requestClient = getRequestClient();
			String dataJson = requestClient.ping();
			return GimBroadcastManager.parsePingData(dataJson);
		}
		catch (Exception e)
		{
			log.error(e.toString());
			return new HashMap<>();
		}
	}
}
