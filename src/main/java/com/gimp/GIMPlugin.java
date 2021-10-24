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
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

import javax.inject.Inject;
import java.util.*;

@Slf4j
@PluginDescriptor(
	name = "GIMP"
)
public class GIMPlugin extends Plugin
{
	final private GIMPIconProvider iconProvider = new GIMPIconProvider();

	private GIMPLocationManager gimpLocationManager;

	final private List<WorldMapPoint> playerWaypoints = new ArrayList<>();

	@Inject
	private WorldMapPointManager worldMapPointManager;

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
		// If game state changes to the login screen, stop any ongoing tasks
		if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN && gimpTaskManager != null)
		{
			gimpTaskManager.resetTasks();
		}
	}

	// TODO: There must be a more reliable way of detecting joining a GIM clan channel
	@Subscribe
	public void onClanChannelChanged(ClanChannelChanged clanChannelChanged)
	{
		Player localPlayer = client.getLocalPlayer();
		ClanChannel changedClanChannel = clanChannelChanged.getClanChannel();
		if (changedClanChannel != null && localPlayer != null)
		{
			ClanChannel gimClanChannel = client.getClanChannel(ClanID.GROUP_IRONMAN);
			if (gimClanChannel != null)
			{
				String changedClanChannelName = changedClanChannel.getName();
				String gimClanChannelName = gimClanChannel.getName();
				// ClanChannelChanged event does not register joining a Group Ironman clan
				// but checking for it here seems to work
				if (gimClanChannelName.equals(changedClanChannelName))
				{
					// Never reaches this code
					log.debug("GIM clan joined: " + gimClanChannelName);
				}
				else
				{
					log.debug("GIM clan already joined: " + gimClanChannelName);
				}
				List<ClanChannelMember> clanChannelMembers = gimClanChannel.getMembers();
				ArrayList<String> gimpNames = new ArrayList<>();
				for (ClanChannelMember member : clanChannelMembers)
				{
					gimpNames.add(member.getName());
				}
				gimpLocationManager = new GIMPLocationManager(gimpNames);
				startBroadcast();
			}
		}
	}

	private void startBroadcast()
	{
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
						WorldPoint worldPoint = localPlayer.getWorldLocation();
						GIMPLocation location = new GIMPLocation(
							worldPoint.getX(),
							worldPoint.getY(),
							worldPoint.getPlane()
						);
						gimpBroadcastManager.broadcast(localPlayer.getName(), location);
					}
				}

				@Override
				public long delay()
				{
					if (gimpBroadcastManager.isSocketConnected())
					{
						return FIVE_SECONDS / 2;
					}
					return period;
				}
			};
			GIMPTask pingTask = new GIMPTask(FIVE_SECONDS)
			{
				@Override
				public void run()
				{
					Map<String, GIMPLocation> locationData = gimpBroadcastManager.ping();
					gimpLocationManager.update(locationData);
					// TODO: move this logic to its own class for managing the actual map icons
					Map<String, WorldPoint> gimpWorldPoints = gimpLocationManager.getOtherGimpWorldPoints(localPlayer.getName());
					for (WorldMapPoint playerWaypoint : playerWaypoints)
					{
						worldMapPointManager.removeIf(x -> x == playerWaypoint);
					}
					for (String name : gimpWorldPoints.keySet())
					{
						WorldPoint worldPoint = gimpWorldPoints.get(name);
						WorldMapPoint playerWaypoint = new WorldMapPoint(worldPoint, iconProvider.getIcon(name));
						playerWaypoints.add(playerWaypoint);
						playerWaypoint.setTarget(playerWaypoint.getWorldPoint());
						worldMapPointManager.add(playerWaypoint);
					}
				}

				@Override
				public long delay()
				{
					// Only ping every 10 seconds if world map closed
					final Widget worldMapView = client.getWidget(WidgetInfo.WORLD_MAP_VIEW);
					if (worldMapView == null)
					{
						return FIVE_SECONDS * 2;
					}
					if (gimpBroadcastManager.isSocketConnected())
					{
						return FIVE_SECONDS / 2;
					}
					return period;
				}
			};
			gimpTaskManager.schedule(broadcastTask, 0);
			gimpTaskManager.schedule(pingTask, FIVE_SECONDS / 2);
			gimpTaskManager.schedule(socketConnectTask, FIVE_SECONDS * 2);
		}
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
