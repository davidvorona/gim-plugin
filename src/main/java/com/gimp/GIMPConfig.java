package com.gimp;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("example")
public interface GIMPConfig extends Config
{
	@ConfigItem(
		keyName = "serverIp",
		name = "Server IP",
		description = "IP of the remote GIMP server"
	)
	default String serverIp()
	{
		return "";
	}

	@ConfigItem(
		keyName = "serverPort",
		name = "Server Port",
		description = "Port of the remote GIMP server"
	)
	default String serverPort()
	{
		return "";
	}
}
