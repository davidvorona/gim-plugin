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

import com.gimp.gimps.*;
import com.gimp.tasks.*;
import com.google.inject.Provides;
import io.socket.emitter.Emitter;
import java.awt.image.BufferedImage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanID;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import javax.inject.Inject;
import java.util.*;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import org.json.JSONObject;

@Slf4j
@PluginDescriptor(
	name = "GIMP"
)
public class GimPlugin extends Plugin
{
	@Inject
	private TaskManager taskManager;

	@Inject
	private GimBroadcastManager gimBroadcastManager;

	@Inject
	@Getter
	private Group group;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private GimPluginConfig config;

	private GimPluginPanel panel;

	private NavigationButton navButton;

	@Override
	protected void startUp()
	{
		log.debug("GIMP started!");
		addPanel();
	}

	@Override
	protected void shutDown()
	{
		log.debug("GIMP stopped!");
		removePanel();
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		// Don't bother checking until gimps are loaded
		GimPlayer localGimp = group.getLocalGimp();
		if (localGimp != null)
		{
			final int currentHp = client.getBoostedSkillLevel(Skill.HITPOINTS);
			final int currentPrayer = client.getBoostedSkillLevel(Skill.PRAYER);
			final int lastHp = localGimp.getHp();
			// If HP value has changed, broadcast
			if (currentHp != lastHp)
			{
				Map<String, Object> hpData = localGimp.getData();
				hpData.put("hp", currentHp);
				gimBroadcastManager.broadcast(hpData);
			}
			final int lastPrayer = localGimp.getPrayer();
			// If prayer value has changed, broadcast
			if (currentPrayer != lastPrayer)
			{
				Map<String, Object> prayerData = localGimp.getData();
				prayerData.put("prayer", currentPrayer);
				gimBroadcastManager.broadcast(prayerData);
			}
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		// Don't bother checking until gimps are loaded
		GimPlayer localGimp = group.getLocalGimp();
		if (localGimp != null)
		{
			if (statChanged.getSkill() == Skill.HITPOINTS)
			{
				final int currentMaxHp = client.getRealSkillLevel(Skill.HITPOINTS);
				final int lastMaxHp = localGimp.getMaxHp();
				// If max (real) HP value has changed, broadcast
				if (currentMaxHp != lastMaxHp)
				{
					Map<String, Object> hpData = localGimp.getData();
					hpData.put("maxHp", currentMaxHp);
					gimBroadcastManager.broadcast(hpData);
				}
			}
			if (statChanged.getSkill() == Skill.PRAYER)
			{
				final int currentMaxPrayer = client.getRealSkillLevel(Skill.PRAYER);
				final int lastMaxPrayer = localGimp.getMaxPrayer();
				// If max (real) prayer value has changed, broadcast
				if (currentMaxPrayer != lastMaxPrayer)
				{
					Map<String, Object> prayerData = localGimp.getData();
					prayerData.put("maxPrayer", currentMaxPrayer);
					gimBroadcastManager.broadcast(prayerData);
				}
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		// If game state changes to the login screen or hopping, or connection is lost, stop the broadcast
		GameState gameState = gameStateChanged.getGameState();
		if (
			gameState == GameState.LOGIN_SCREEN
				|| gameState == GameState.HOPPING
				|| gameState == GameState.CONNECTION_LOST
		)
		{
			group.unload();
			panel.unload();
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
				group.load();
				// Once group is loaded, we can display panel and start the broadcast
				group.waitForLoad(() ->
				{
					panel.load();
					startBroadcast();
				});
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		String CONFIG_GROUP = "gimp";
		String SERVER_IP_KEY = "serverIp";
		String SERVER_PORT_KEY = "serverPort";
		// Check if one of GIMP's server IP/port config values has changed
		if (
			configChanged.getGroup().equals(CONFIG_GROUP)
				&& (configChanged.getKey().equals(SERVER_IP_KEY)
				|| configChanged.getKey().equals(SERVER_PORT_KEY))
		)
		{
			if (gimBroadcastManager.isSocketConnected())
			{
				// If socket is currently connected, disconnect and let it reconnect with new IP/port
				log.debug("Server IP/port changed, disconnecting socket client");
				gimBroadcastManager.disconnectSocketClient();
			}
		}
	}

	private void addPanel()
	{
		// Panel must be injected this way to avoid UI inconsistencies
		panel = injector.getInstance(GimPluginPanel.class);
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "gimpoint-small.png");
		// This is pretty arbitrary, but currently places the nav button at
		// the bottom of the list if there are no third-party plugin panels
		int lowPriority = 15;
		navButton = NavigationButton.builder()
			.tooltip("GIMP")
			.icon(icon)
			.priority(lowPriority)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
	}

	private void removePanel()
	{
		clientToolbar.removeNavigation(navButton);
	}

	private void startBroadcast()
	{
		log.debug("Starting broadcast...");
		gimBroadcastManager.connectSocketClient();
		// Start listening for server broadcast
		listenForBroadcast();
		// Start interval-based broadcast tasks
		startIntervalTasks();
	}

	private void listenForBroadcast()
	{
		gimBroadcastManager.listen(new Emitter.Listener()
		{
			@Override
			public void call(Object... args)
			{
				JSONObject dataJson = (JSONObject) args[0];
				GimPlayer gimpData = GimBroadcastManager.parseBroadcastData(dataJson.toString());
				group.update(gimpData);
			}
		});
	}

	private void startIntervalTasks()
	{
		// Sanity check
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer != null)
		{
			long FIVE_SECONDS = 5000;
			Task socketConnectTask = new Task(FIVE_SECONDS * 2)
			{
				@Override
				public void run()
				{
					if (!gimBroadcastManager.isSocketConnected())
					{
						gimBroadcastManager.connectSocketClient();
					}
				}
			};
			Task locationBroadcastTask = new Task(FIVE_SECONDS)
			{
				@Override
				public void run()
				{
					if (!config.ghostMode())
					{
						GimLocation gimLocation = new GimLocation(localPlayer.getWorldLocation());
						Map<String, Object> data = group.getLocalGimp().getData();
						data.put("location", gimLocation.getLocation());
						gimBroadcastManager.broadcast(data);
					}
				}

				@Override
				public long delay()
				{
					if (gimBroadcastManager.isSocketConnected())
					{
						return period / 2;
					}
					return period;
				}
			};
			Task httpFallbackPingTask = new Task(FIVE_SECONDS * 2)
			{
				@Override
				public void run()
				{
					// If socket is not connected, fetch data (instead of waiting for broadcast)
					if (!gimBroadcastManager.isSocketConnected())
					{
						Map<String, GimPlayer> gimData = gimBroadcastManager.ping();
						for (GimPlayer gimp : group.getGimps())
						{
							GimPlayer gimpData = gimData.get(gimp.getName());
							group.update(gimpData);
						}
					}
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
					if (gimBroadcastManager.isSocketConnected())
					{
						nextDelay = nextDelay / 2;
					}
					return nextDelay;
				}
			};
			taskManager.schedule(locationBroadcastTask, 0);
			taskManager.schedule(httpFallbackPingTask, FIVE_SECONDS / 2);
			taskManager.schedule(socketConnectTask, FIVE_SECONDS * 2);
		}
	}

	private void stopBroadcast()
	{
		log.debug("Stopping broadcast...");
		taskManager.resetTasks();
	}

	@Provides
	GimPluginConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GimPluginConfig.class);
	}
}
