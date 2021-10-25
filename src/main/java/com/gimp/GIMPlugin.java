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

import com.gimp.locations.*;
import com.gimp.tasks.*;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanID;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.ClanMemberJoined;
import net.runelite.api.events.ClanMemberLeft;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.util.*;

@Slf4j
@PluginDescriptor(
	name = "GIMP"
)
public class GIMPlugin extends Plugin
{
	@Inject
	private GIMPLocationManager gimpLocationManager;

	@Inject
	private GIMPTaskManager gimpTaskManager;

	@Inject
	private GIMPBroadcastManager gimpBroadcastManager;

	@Inject
	private Client client;

	@Inject
	private GIMPConfig config;

	@Override
	protected void startUp()
	{
		log.debug("GIMP started!");
	}

	@Override
	protected void shutDown()
	{
		log.debug("GIMP stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		// If game state changes to the login screen or hopping, stop the broadcast
		GameState gameState = gameStateChanged.getGameState();
		if (
			gameState == GameState.LOGIN_SCREEN
				|| gameState == GameState.HOPPING
		)
		{
			stopBroadcast();
		}
	}

	@Subscribe
	public void onClanChannelChanged(ClanChannelChanged clanChannelChanged)
	{
		Player localPlayer = client.getLocalPlayer();
		ClanChannel changedClanChannel = clanChannelChanged.getClanChannel();
		if (changedClanChannel != null && localPlayer != null)
		{
			ClanChannel gimClanChannel = client.getClanChannel(ClanID.GROUP_IRONMAN);
			if (changedClanChannel == gimClanChannel)
			{
				String gimClanChannelName = gimClanChannel.getName();
				log.debug("GIM clan joined: " + gimClanChannelName);
				startBroadcast();
			}
		}
	}

	// TODO: Send ping request if GIMP joins
	public void onClanMemberJoined(ClanMemberJoined clanMemberJoined)
	{
		log.debug(clanMemberJoined.getClanChannel().getName()
			+ ": " + clanMemberJoined.getClanMember().getName() + " joined");
	}

	// TODO: 	Remove GIMPLocation from location manager if member leaves - how to deal
	// 			with server still having location of offline member?
	public void onClanMemberLeft(ClanMemberLeft clanMemberLeft)
	{
		log.debug(clanMemberLeft.getClanChannel().getName()
			+ ": " + clanMemberLeft.getClanMember().getName() + " joined");
	}

	private void startBroadcast()
	{
		log.debug("Starting broadcast...");
		gimpBroadcastManager.connectSocketClient();
		// Sanity check
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer != null)
		{
			long FIVE_SECONDS = 5000;
			GIMPTask socketConnectTask = new GIMPTask(FIVE_SECONDS * 2)
			{
				@Override
				public void run()
				{
					if (!gimpBroadcastManager.isSocketConnected())
					{
						gimpBroadcastManager.connectSocketClient();
					}
				}
			};
			GIMPTask broadcastTask = new GIMPTask(FIVE_SECONDS)
			{
				@Override
				public void run()
				{
					if (!config.ghostMode())
					{
						GIMPLocation location = gimpLocationManager.getCurrentLocation(localPlayer);
						gimpBroadcastManager.broadcast(localPlayer.getName(), location);
					}
				}

				@Override
				public long delay()
				{
					if (gimpBroadcastManager.isSocketConnected())
					{
						return period / 2;
					}
					return period;
				}
			};
			GIMPTask pingTask = new GIMPTask(FIVE_SECONDS * 2)
			{
				@Override
				public void run()
				{
					Map<String, GIMPLocation> locationData = gimpBroadcastManager.ping();
					gimpLocationManager.updateLocations(locationData);
					gimpLocationManager.updateMapPoints(localPlayer);
				}

				@Override
				public long delay()
				{
					// Start with default period
					long nextDelay = period;
					final Widget worldMapView = client.getWidget(WidgetInfo.WORLD_MAP_VIEW);
					// Quarter delay to 2.5 secs if world map is open
					if (worldMapView != null)
					{
						nextDelay = nextDelay / 4;
					}
					// Half delay if socket is connected: 1.25 secs if map open, 5 secs if closed
					if (gimpBroadcastManager.isSocketConnected())
					{
						nextDelay = nextDelay / 2;
					}
					return nextDelay;
				}
			};
			gimpTaskManager.schedule(broadcastTask, 0);
			gimpTaskManager.schedule(pingTask, FIVE_SECONDS / 2);
			gimpTaskManager.schedule(socketConnectTask, FIVE_SECONDS * 2);
		}
	}

	private void stopBroadcast()
	{
		log.debug("Stopping broadcast...");
		gimpTaskManager.resetTasks();
		gimpLocationManager.purgeLocations();
		gimpLocationManager.clearMapPoints();
	}

	@Provides
	GIMPConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GIMPConfig.class);
	}

	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GIMPlugin.class);
		RuneLite.main(args);
	}
}
