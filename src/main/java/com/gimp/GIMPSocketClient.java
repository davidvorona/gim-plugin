package com.gimp;

import io.socket.client.Socket;
import io.socket.engineio.client.transports.Polling;
import io.socket.engineio.client.transports.WebSocket;
import java.net.*;
import io.socket.client.IO;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class GIMPSocketClient
{
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

		socket.on("pong", args -> {
			log.info(String.valueOf(args));
		});
	}

	public void send(String msg)
	{
		socket.emit(msg);
	}
}
