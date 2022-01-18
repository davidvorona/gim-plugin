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

import com.gimp.gimps.GimLocation;
import com.gimp.gimps.GimPlayer;
import com.gimp.gimps.Group;
import com.gimp.map.GimWorldMapPoint;
import com.gimp.map.GimWorldMapPointManager;
import com.gimp.tasks.Task;
import com.gimp.tasks.TaskManager;
import com.google.inject.Provides;
import io.socket.emitter.Emitter;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.inject.Inject;
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
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
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
	public final static int OFFLINE_WORLD = 0;
	private final static int MAP_POINT_TICK_PERIOD = 300;

	@Inject
	private TaskManager taskManager;

	@Inject
	@Getter
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private GimPluginConfig config;

	@Inject
	@Getter
	private Group group;

	@Inject
	private GimWorldMapPointManager gimWorldMapPointManager;

	private GimBroadcastManager gimBroadcastManager;

	private GimPluginPanel panel;

	private NavigationButton navButton;

	/**
	 * Toggle to determine if we're on an "even" frame or an "odd" frame of map point ticking.
	 * This is the simplest way to keep map point motion smooth and consistent, as we can either
	 * move by one tile each frame (fast) or move by one tile every other frame (slow),
	 * without any variation in delay or distance between each frame.
	 */
	private boolean frameToggle;

	final private Emitter.Listener onBroadcastReconnect = new Emitter.Listener()
	{
		@Override
		public void call(Object... args)
		{
			// Update local gimp
			group.localUpdate();
			// Must be invoked later or it blocks this thread
			clientThread.invokeLater(() ->
			{
				// Send out broadcast
				gimBroadcastManager.broadcast(group.getLocalGimp().getGimpData());
				// Ping for initial gimp data
				pingForUpdate();
			});
		}
	};

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

	/* EVENTS */

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
			stopBroadcast();
			gimWorldMapPointManager.clear();
			panel.unload();
			group.unload();
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
				// Once group is loaded, we can display panel and start the broadcast
				group.load().whenCompleteAsync((result, ex) ->
				{
					panel.load();
					startBroadcast();
				});
			}
		}
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
			// If HP value has changed, update
			if (currentHp != localGimp.getHp())
			{
				updateHp(currentHp);
			}
			// If prayer value has changed, update
			if (currentPrayer != localGimp.getPrayer())
			{
				updatePrayer(currentPrayer);
			}
			// If any gimp world / online status has changed, update
			for (GimPlayer gimp : group.getGimps())
			{
				final int currentWorld = group.getCurrentWorld(gimp.getName());
				final int lastWorld = gimp.getWorld();
				if (currentWorld != lastWorld)
				{
					updateWorld(gimp, currentWorld);
					// If logging in or out, update last activity panel text
					if (currentWorld == 0 || lastWorld == 0)
					{
						panel.setLastActivity(gimp.getName(), gimp.getLastActivity(), currentWorld);
					}
				}
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
			String activity = statChanged.getSkill().toString();
			final int currentHp = client.getBoostedSkillLevel(Skill.HITPOINTS);
			final int currentMaxHp = client.getRealSkillLevel(Skill.HITPOINTS);
			final int currentPrayer = client.getBoostedSkillLevel(Skill.PRAYER);
			final int currentMaxPrayer = client.getRealSkillLevel(Skill.PRAYER);
			if (
				// Except if the gimp's HP is going up/down
				!(statChanged.getSkill() == Skill.HITPOINTS && currentHp != localGimp.getHp())
					// or if the gimp's prayer is going up/down
					&& !(statChanged.getSkill() == Skill.PRAYER && currentPrayer != localGimp.getPrayer())
			)
			{
				// If max (real) HP value has changed, broadcast
				if (statChanged.getSkill() == Skill.HITPOINTS && currentMaxHp != localGimp.getMaxHp())
				{
					updateMaxHp(currentMaxHp);
				}
				// If max (real) prayer value has changed, broadcast
				else if (statChanged.getSkill() == Skill.PRAYER && currentMaxPrayer != localGimp.getMaxPrayer())
				{
					updateMaxPrayer(currentMaxPrayer);
				}
				// Process XP changed for last activity update, except never Hitpoints
				else if (
					(localGimp.getLastActivity() == null || !activity.equals(localGimp.getLastActivity()))
						&& statChanged.getSkill() != Skill.HITPOINTS
				)
				{
					updateLastActivity(activity);
				}
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		String CONFIG_GROUP = "gimp";
		String SERVER_ADDRESS_KEY = "serverAddress";
		String GHOST_MODE = "ghostMode";
		// Check if one of GIMP's server address config value has changed
		if (
			configChanged.getGroup().equals(CONFIG_GROUP)
				&& configChanged.getKey().equals(SERVER_ADDRESS_KEY)
		)
		{
			if (gimBroadcastManager != null && gimBroadcastManager.isSocketConnected())
			{
				// If socket is currently connected, disconnect and let it reconnect with new address
				log.debug("Server address changed, disconnecting socket client");
				gimBroadcastManager.disconnectSocketClient();
			}
		}
		else if (
			configChanged.getGroup().equals(CONFIG_GROUP)
				&& configChanged.getKey().equals(GHOST_MODE)
		)
		{
			updateGhostMode(config.ghostMode());
		}
	}

	/* PLUGIN PANEL */

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

	/* MAIN ACTIONS */

	/**
	 * Starts the main broadcast, connecting to the socket
	 * client and sending out the initial ping for data / broadcast.
	 * It also starts all broadcast-related interval tasks.
	 */
	private void startBroadcast()
	{
		log.debug("Starting broadcast...");
		gimBroadcastManager = new GimBroadcastManager(group.getName(), config);
		gimBroadcastManager.connectSocketClient();
		// Send out initial broadcast
		gimBroadcastManager.broadcast(group.getLocalGimp().getGimpData());
		// Ping for initial gimp data
		pingForUpdate();
		// Start listening for server broadcast
		listenForBroadcast();
		// Start interval-based broadcast tasks
		startIntervalTasks();
	}

	/**
	 * Listens for the socket "broadcast" event and updates
	 * gimp data.
	 */
	private void listenForBroadcast()
	{
		gimBroadcastManager.listen(new Emitter.Listener()
		{
			@Override
			public void call(Object... args)
			{
				JSONObject dataJson = (JSONObject) args[0];
				log.debug(dataJson.toString());
				GimPlayer gimpData = GimBroadcastManager.parseBroadcastData(dataJson.toString());
				handleUpdate(gimpData);
			}
		});
	}

	/**
	 * Starts all broadcast interval tasks, including handling socket
	 * reconnects, broadcasting location, and pinging for gimp data
	 * via HTTP if sockets fail.
	 */
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
						gimBroadcastManager.onBroadcastConnect(onBroadcastReconnect);
					}
				}
			};
			Task locationBroadcastTask = new Task(FIVE_SECONDS)
			{
				@Override
				public void run()
				{
					final GimPlayer localGimp = group.getLocalGimp();
					if (localGimp != null)
					{
						GimLocation gimLocation = new GimLocation(localPlayer.getWorldLocation());
						GimLocation lastLocation = localGimp.getLocation();
						// Don't update location if it hasn't changed
						if (lastLocation != null && GimLocation.compare(lastLocation, gimLocation))
						{
							return;
						}
						updateLocation(gimLocation);
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
						pingForUpdate();
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
			Task tickMapPoints = new Task(MAP_POINT_TICK_PERIOD)
			{
				@Override
				public void run()
				{
					frameToggle = !frameToggle;
					for (GimPlayer gimp : group.getGimps())
					{
						if (gimp != null)
						{
							refreshMapPointVisibility(gimp);
							if (gimWorldMapPointManager.hasPoint(gimp.getName()))
							{
								final GimWorldMapPoint gimWorldMapPoint = gimWorldMapPointManager.getPoint(gimp.getName());
								final boolean moved = gimWorldMapPoint.moveTowardPlayer(frameToggle);
								if (moved)
								{
									gimWorldMapPoint.addFootstep(gimWorldMapPointManager, config.showFootsteps());
								}
							}
						}
					}
				}
			};
			taskManager.schedule(locationBroadcastTask, 0);
			taskManager.schedule(httpFallbackPingTask, FIVE_SECONDS / 2);
			taskManager.schedule(socketConnectTask, FIVE_SECONDS * 2);
			taskManager.schedule(tickMapPoints, 0);
		}
	}

	/**
	 * Sends a ping via HTTP or socket for all server gimp data. Sent
	 * when the broadcast starts and as a fallback if the socket disconnects.
	 */
	private void pingForUpdate()
	{
		Map<String, GimPlayer> gimData = gimBroadcastManager.ping();
		if (gimData != null)
		{
			for (GimPlayer gimp : group.getGimps())
			{
				GimPlayer gimpData = gimData.get(gimp.getName());
				// TODO: Some data for the local gimp needs to come from the server, so
				// we comment out this condition. Instead, we need to implement a method
				// that hydrates empty data on a GimPlayer but does not overwrite existing.
				if (gimpData != null/*  && gimp != group.getLocalGimp()*/)
				{
					handleUpdate(gimpData);
				}
			}
		}
	}

	/**
	 * Stops the broadcast, canceling all broadcast-related
	 * behaviors that are not initiated by RuneLite events.
	 */
	private void stopBroadcast()
	{
		log.debug("Stopping broadcast...");
		taskManager.resetTasks();
		if (gimBroadcastManager != null)
		{
			gimBroadcastManager.stopListening();
			gimBroadcastManager.disconnectSocketClient();
		}
	}

	/* UPDATE FUNCTIONS */

	/**
	 * Handles an update from the server, maps gimp data to the
	 * corresponding gimp and updates the panel.
	 *
	 * @param gimpData GimPlayer data
	 */
	private void handleUpdate(GimPlayer gimpData)
	{
		group.update(gimpData);
		panel.updateGimpData(gimpData);
	}

	/**
	 * Updates the local gimp HP value and broadcasts
	 * the change.
	 *
	 * @param hp HP value of the local GimPlayer
	 */
	private void updateHp(int hp)
	{
		GimPlayer localGimp = group.getLocalGimp();
		if (localGimp != null)
		{
			// Set it locally first, to prevent loops
			localGimp.setHp(hp);
			panel.updateGimpData(localGimp);
			// Broadcast new HP value
			Map<String, Object> hpData = localGimp.getData();
			hpData.put("hp", hp);
			gimBroadcastManager.broadcast(hpData);
		}
	}

	/**
	 * Updates the local gimp max HP value and broadcasts
	 * the change.
	 *
	 * @param maxHp max HP value of the local GimPlayer
	 */
	private void updateMaxHp(int maxHp)
	{
		GimPlayer localGimp = group.getLocalGimp();
		if (localGimp != null)
		{
			// Set it locally first, to prevent loops
			localGimp.setMaxHp(maxHp);
			panel.updateGimpData(localGimp);
			// Broadcast new max HP value
			Map<String, Object> hpData = localGimp.getData();
			hpData.put("maxHp", maxHp);
			gimBroadcastManager.broadcast(hpData);
		}
	}

	/**
	 * Updates the local gimp prayer value and broadcasts
	 * the change.
	 *
	 * @param prayer prayer value of the local GimPlayer
	 */
	private void updatePrayer(int prayer)
	{
		GimPlayer localGimp = group.getLocalGimp();
		if (localGimp != null)
		{
			// Set it locally first, to prevent loops
			localGimp.setPrayer(prayer);
			panel.updateGimpData(localGimp);
			// Broadcast new prayer value
			Map<String, Object> prayerData = localGimp.getData();
			prayerData.put("prayer", prayer);
			gimBroadcastManager.broadcast(prayerData);
		}
	}

	/**
	 * Updates the local gimp max prayer value and broadcasts
	 * the change.
	 *
	 * @param maxPrayer max prayer value of the local GimPlayer
	 */
	private void updateMaxPrayer(int maxPrayer)
	{
		GimPlayer localGimp = group.getLocalGimp();
		if (localGimp != null)
		{
			// Set it locally first, to prevent loops
			localGimp.setMaxPrayer(maxPrayer);
			panel.updateGimpData(localGimp);
			// Broadcast new max prayer value
			Map<String, Object> prayerData = localGimp.getData();
			prayerData.put("maxPrayer", maxPrayer);
			gimBroadcastManager.broadcast(prayerData);
		}
	}

	/**
	 * Updates the world of provided GimPlayer.
	 *
	 * @param world world number of GimPlayer
	 */
	private void updateWorld(GimPlayer gimp, int world)
	{
		group.setWorld(gimp.getName(), world);
		panel.setWorld(gimp.getName(), world);
	}

	/**
	 * Updates the local gimp ghost mode value and broadcasts
	 * the change to the server. If ghost mode is turned off,
	 * includes all local gimp data in broadcast.
	 *
	 * @param ghostMode ghost mode setting of local GimPlayer
	 */
	private void updateGhostMode(boolean ghostMode)
	{
		GimPlayer localGimp = group.getLocalGimp();
		if (localGimp != null)
		{
			// Set new ghost mode locally before broadcast
			group.setGhostMode(localGimp.getName(), ghostMode);
			Map<String, Object> ghostModeData = ghostMode
				? localGimp.getData()
				: localGimp.getGimpData(); // if ghostMode off, broadcast all data
			ghostModeData.put("ghostMode", ghostMode);
			gimBroadcastManager.broadcast(ghostModeData);
		}
	}

	/**
	 * Updates the local gimp location and broadcasts the change
	 * if ghost mode is not enabled.
	 *
	 * @param gimLocation world location of local GimPlayer
	 */
	private void updateLocation(GimLocation gimLocation)
	{
		GimPlayer localGimp = group.getLocalGimp();
		if (localGimp != null)
		{
			// Set location locally before broadcast
			group.setLocation(localGimp.getName(), gimLocation);
			panel.updateGimpData(localGimp);
			// Do not broadcast location at all if ghost mode is active
			if (!config.ghostMode())
			{
				Map<String, Object> data = localGimp.getData();
				data.put("location", gimLocation.getLocation());
				gimBroadcastManager.broadcast(data);
			}
		}
	}

	private void updateLastActivity(String activity)
	{
		GimPlayer localGimp = group.getLocalGimp();
		if (localGimp != null)
		{
			// Set activity locally before broadcast
			localGimp.setLastActivity(activity);
			panel.updateGimpData(localGimp);
			Map<String, Object> activityData = localGimp.getData();
			activityData.put("lastActivity", activity);
			gimBroadcastManager.broadcast(activityData);
		}
	}

	/**
	 * Determine if the given player's world map point should be displayed or not,
	 * then either add or remove it accordingly.
	 *
	 * @param gimp the player whose map point is to be refreshed
	 */
	private void refreshMapPointVisibility(GimPlayer gimp)
	{
		final String name = gimp.getName();
		final boolean isLocalGimp = gimp == group.getLocalGimp();
		final boolean shouldShow =
			// Condition 1: Player must have a location
			gimp.getLocation() != null
				// Condition 2: Must be another player (unless "show self" is on)
				&& (!isLocalGimp || config.showSelf())
				// Condition 3: Must not be in ghost mode (unless it's the local player)
				&& (gimp.shouldIncludeLocation() || isLocalGimp)
				// Condition 4: Must be logged in
				&& gimp.getWorld() != OFFLINE_WORLD;
		// Add or remove the player's world map point accordingly
		if (shouldShow && !gimWorldMapPointManager.hasPoint(name))
		{
			gimWorldMapPointManager.addPoint(gimp);
		}
		else if (!shouldShow && gimWorldMapPointManager.hasPoint(name))
		{
			gimWorldMapPointManager.removePoint(name);
		}
	}

	@Provides
	GimPluginConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GimPluginConfig.class);
	}

	// Support testing via Gradle "run" task
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GimPlugin.class);
		RuneLite.main(args);
	}
}
