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
import io.socket.client.Ack;
import io.socket.client.Socket;
import io.socket.engineio.client.transports.Polling;
import io.socket.engineio.client.transports.WebSocket;
import java.net.URI;
import io.socket.client.IO;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

@Getter
@Slf4j
public class SocketClient extends RequestClient
{
	public Socket client;

	private static final String EVENT_CONNECTION_ACK = "connection-ack";

	public SocketClient(String namespace, GimPluginConfig config)
	{
		this.namespace = namespace;
		this.config = config;
	}

	/**
	 * Connects the socket to the server at the base URL, using default config
	 * for the connection. On connection, sets up socket listeners for socket
	 * lifecycle events, e.g. connect, disconnect, connect_error.
	 */
	public void connect()
	{
		if (!validateUrl())
		{
			log.warn("Invalid socket URL, aborting");
			return;
		}
		URI uri = URI.create(getBaseUrl());
		IO.Options options = IO.Options.builder()
			// IO factory options
			.setForceNew(false).setMultiplex(true)

			// low-level engine options
			.setTransports(new String[]{Polling.NAME, WebSocket.NAME}).setUpgrade(true).setRememberUpgrade(false).setPath("/socket.io/").setQuery(null).setExtraHeaders(null)

			// Manager options
			.setReconnection(true).setReconnectionAttempts(Integer.MAX_VALUE).setReconnectionDelay(1_000).setReconnectionDelayMax(5_000).setRandomizationFactor(0.5).setTimeout(20_000)

			// Socket options
			.setAuth(null).build();
		if (client != null)
		{
			client.close();
		}
		client = IO.socket(uri, options);
		client.connect();

		client.on(Socket.EVENT_CONNECT, args -> {
			log.debug("Socket connected");
			String roomId = namespace;
			client.emit(EVENT_CONNECTION_ACK, roomId);
		});

		client.on(Socket.EVENT_DISCONNECT, args -> {
			log.debug("Socket disconnected");
		});

		client.on(Socket.EVENT_CONNECT_ERROR, args -> {
			log.warn("Failed to connect to socket server, closing");
			client.close();
		});
	}

	/**
	 * Disconnects the client from the socket server.
	 */
	public void disconnect()
	{
		if (client != null)
		{
			client.disconnect();
		}
	}

	/**
	 * Checks if the client is connected to a socket server.
	 *
	 * @return whether socket is connected
	 */
	public boolean isConnected()
	{
		if (client != null)
		{
			return client.connected();
		}
		return false;
	}

	/**
	 * Sends a socket message to the ping listener. Expects an acknowledgement
	 * from the server, and returns the JSON data in that acknowledgement.
	 *
	 * @return future of ack data in JSON
	 */
	public CompletableFuture<String> ping()
	{
		String EVENT_PING = "ping";
		CompletableFuture<String> socketResponse = new CompletableFuture<>();
		client.emit(EVENT_PING, (Ack) args -> {
			JSONObject data = (JSONObject) args[0];
			socketResponse.complete(data.toString());
		});
		return socketResponse;
	}

	/**
	 * Sends a socket message to the broadcast listener. Passes the JSON data
	 * as the data parameter and expects an acknowledgement from the server.
	 *
	 * @param dataJson future of ack data in JSON
	 * @return future of ack data in JSON
	 */
	public CompletableFuture<String> broadcast(String dataJson)
	{
		String EVENT_BROADCAST = "broadcast";
		CompletableFuture<String> socketResponse = new CompletableFuture<>();
		client.emit(EVENT_BROADCAST, dataJson, (Ack) args -> {
			JSONObject data = (JSONObject) args[0];
			socketResponse.complete(data.toString());
		});
		return socketResponse;
	}
}
