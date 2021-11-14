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

import com.gimp.GIMPConfig;
import java.net.URL;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class GIMPRequestClient
{
	@Inject
	public GIMPConfig config;

	/**
	 * Gets the base URL from the IP and port injected from the plugin config.
	 *
	 * @return server base URL
	 */
	public String getBaseUrl()
	{
		return "http://" + config.serverIp() + ":" + config.serverPort();
	}

	/**
	 * Validates the URL from the config IP and port.
	 *
	 * @return whether the URL is valid
	 */
	public boolean validateUrl()
	{
		final URL url;
		if (config.serverIp().isEmpty() || config.serverPort().isEmpty())
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
	 * Pings server for location of fellow GIMPs
	 *
	 * @return response data in JSON
	 */
	abstract public String ping() throws Exception;

	/**
	 * Broadcasts local GIMP's current location to server
	 *
	 * @param dataJson request data in JSON
	 */
	abstract public void broadcast(String dataJson) throws Exception;
}
