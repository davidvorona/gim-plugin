package com.gimp;

import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class GIMPRequestClient
{
	@Inject
	public GIMPConfig config;

	public String getBaseUrl()
	{
		return "http://" + config.serverIp() + ":" + config.serverPort();
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
		if (!ip.equals("localhost"))
		{
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

	/**
	 * Pings server for location of fellow GIMPs
	 *
	 * @return response data in JSON
	 */
	abstract String ping() throws ExecutionException, InterruptedException, TimeoutException, URISyntaxException;

	/**
	 * Broadcasts local GIMP's current location to server
	 *
	 * @param dataJson request data in JSON
	 */
	abstract void broadcast(String dataJson) throws ExecutionException, InterruptedException, URISyntaxException;
}
