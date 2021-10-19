package com.gimp;

import io.socket.client.Ack;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.Polling;
import io.socket.engineio.client.transports.WebSocket;
import java.net.*;
import io.socket.client.IO;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

@Slf4j
public class GIMPSocketClient extends GIMPRequestClient
{
	@Getter(AccessLevel.PACKAGE)
	public Socket client;

	public void connect()
	{
		URI uri = URI.create(getBaseUrl());
		IO.Options options = IO.Options.builder()
			// IO factory options
			.setForceNew(false)
			.setMultiplex(true)

			// low-level engine options
			.setTransports(new String[]{Polling.NAME, WebSocket.NAME})
			.setUpgrade(true)
			.setRememberUpgrade(false)
			.setPath("/socket.io/")
			.setQuery(null)
			.setExtraHeaders(null)

			// Manager options
			.setReconnection(true)
			.setReconnectionAttempts(Integer.MAX_VALUE)
			.setReconnectionDelay(1_000)
			.setReconnectionDelayMax(5_000)
			.setRandomizationFactor(0.5)
			.setTimeout(20_000)

			// Socket options
			.setAuth(null)
			.build();
		client = IO.socket(uri, options);
		client.connect();

		client.on(Socket.EVENT_CONNECT, new Emitter.Listener()
		{
			@Override
			public void call(Object... args)
			{
				log.info(client.id() + " connected");
			}
		});

		client.on(Socket.EVENT_DISCONNECT, new Emitter.Listener()
		{
			@Override
			public void call(Object... args)
			{
				log.info(client.id() + " disconnected"); // null
			}
		});
	}

	public boolean isConnected()
	{
		return client.connected();
	}

	public String ping() throws ExecutionException, InterruptedException, TimeoutException
	{
		String EVENT_PING = "ping";
		CompletableFuture<String> socketResponse = new CompletableFuture<>();
		client.emit(EVENT_PING, new Ack()
		{
			@Override
			public void call(Object... args)
			{
				JSONObject data = (JSONObject) args[0];
				log.info(data.toString());
				socketResponse.complete(data.toString());
			}
		});
		return socketResponse.get(5, TimeUnit.SECONDS);
	}

	public void broadcast(String data)
	{
		String EVENT_BROADCAST = "broadcast";
		client.emit(EVENT_BROADCAST, data);
	}
}
