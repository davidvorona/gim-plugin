package com.gimp.requests;

import com.gimp.GIMPConfig;
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
	 * Validates the IP and port provided by the plugin config, using
	 * basic string validation. The IP and port are validated independently.
	 *
	 * @return whether IP and port are valid
	 */
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
	abstract public String ping() throws Exception;

	/**
	 * Broadcasts local GIMP's current location to server
	 *
	 * @param dataJson request data in JSON
	 */
	abstract public void broadcast(String dataJson) throws Exception;
}
