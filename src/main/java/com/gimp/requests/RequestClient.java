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
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class RequestClient
{
	public GimPluginConfig config;

	/* Used to join a server-side socket room and/or namespace HTTP requests. */
	public String namespace;

	/**
	 * Gets the base URL from the address injected from the plugin config.
	 *
	 * @return server base URL
	 */
	public String getBaseUrl()
	{
		String configServerAddress = config.serverAddress();
		// Remove trailing slash if exists
		if (configServerAddress.endsWith("/"))
		{
			configServerAddress = configServerAddress.substring(0, configServerAddress.length() - 1);
		}
		if (!configServerAddress.contains("http"))
		{
			return "http://" + configServerAddress;
		}
		return configServerAddress;
	}

	/**
	 * Validates the URL from the config server address.
	 *
	 * @return whether the URL is valid
	 */
	public boolean validateUrl()
	{
		final URL url;
		if (config.serverAddress().isEmpty())
		{
			return false;
		}
		try
		{
			url = new URL(getBaseUrl());
		}
		catch (Exception e)
		{
			return false;
		}
		// URL must use HTTP/S protocol
		return url.getProtocol().contains("http");
	}

	/**
	 * Sends a /ping request or emits "ping" to the server.
	 *
	 * @return response data in JSON
	 */
	abstract public CompletableFuture<String> ping();

	/**
	 * Send a /broadcast request or emits "broadcast" to the server.
	 *
	 * @param dataJson request data in JSON
	 */
	abstract public CompletableFuture<String> broadcast(String dataJson);
}
