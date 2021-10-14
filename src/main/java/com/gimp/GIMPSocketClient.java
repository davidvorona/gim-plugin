package com.gimp;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.Polling;
import io.socket.engineio.client.transports.WebSocket;
import java.net.*;
import io.socket.client.IO;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GIMPSocketClient
{
	@Getter(AccessLevel.PACKAGE)
	private Socket socket;

	public void connect(String ip, int port)
	{
		URI uri = URI.create("http://" + ip + ":" + port);
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
		socket = IO.socket(uri, options);
		socket.connect();

		socket.on(Socket.EVENT_CONNECT, new Emitter.Listener()
		{
			@Override
			public void call(Object... args)
			{
				log.info(socket.id() + " connected");
				// TODO: below should work, why doesn't it?
				socket.emit("ping");
			}
		});

		socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener()
		{
			@Override
			public void call(Object... args)
			{
				log.info(socket.id() + " disconnected"); // null
			}
		});

		String EVENT_PONG = "pong";
		socket.on(EVENT_PONG, new Emitter.Listener()
		{
			@Override
			public void call(Object... args)
			{
				log.info("pong");
			}
		});
	}

	public void send(String msg, String data)
	{
		socket.emit(msg, data);
	}
}
