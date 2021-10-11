package com.gimp;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanID;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ImageUtil;
import net.runelite.api.coords.WorldPoint;
import java.awt.image.BufferedImage;
import java.util.*;

@Slf4j
@PluginDescriptor(
	name = "GIMP"
)
public class GIMPlugin extends Plugin
{
	private static final BufferedImage PLAYER_ICON;

	static
	{
		PLAYER_ICON = new BufferedImage(37, 37, BufferedImage.TYPE_INT_ARGB);
		final BufferedImage playerIcon = ImageUtil.loadImageResource(GIMPlugin.class, "waypoint.png");
		PLAYER_ICON.getGraphics().drawImage(playerIcon, 0, 0, null);
	}

	private GIMPLocationManager gimpLocationManager;

	private LocationBroadcastManager locationBroadcastManager;

	private Timer timer;

	@Inject
	private Client client;

	@Inject
	private GIMPConfig config;

	@Override
	protected void startUp() throws Exception
	{
		log.info("GIMP started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("GIMP stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN && timer != null)
		{
			timer.cancel();
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
			if (gimClanChannel != null)
			{
				String changedClanChannelName = changedClanChannel.getName();
				String gimClanChannelName = gimClanChannel.getName();
				// ClanChannelChanged event does not register joining a Group Ironman clan
				// but checking for it here seems to work
				if (gimClanChannelName.equals(changedClanChannelName))
				{
					// Never reaches this code
					log.info("GIM clan joined: " + gimClanChannelName);
				}
				else
				{
					log.info("GIM clan already joined: " + gimClanChannelName);
				}
				List<ClanChannelMember> clanChannelMembers = gimClanChannel.getMembers();
				ArrayList<String> gimpNames = new ArrayList<>();
				for (ClanChannelMember member : clanChannelMembers) {
					gimpNames.add(member.getName());
				}
				gimpLocationManager = new GIMPLocationManager(gimpNames);
				startBroadcast();
			}
			else if (timer != null)
			{
				timer.cancel();
			}
		}
	}

	private void startBroadcast()
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer != null)
		{
			locationBroadcastManager = new LocationBroadcastManager(localPlayer.getName());
			WorldPoint playerLocation = localPlayer.getWorldLocation();
			timer = new Timer();
			TimerTask locationPingTask = new TimerTask()
			{
				@SneakyThrows
				public void run()
				{
					// only ping if world map is open
					final Widget worldMapView = client.getWidget(WidgetInfo.WORLD_MAP_VIEW);
					if (worldMapView != null)
					{
						Map<String, Map<GIMPLocationManager.Coordinate, Integer>> data = locationBroadcastManager.ping();
						// Time to update location in GIMPLocationManager:
						// need to figure out what fields are required for a WorldPoint,
						// and pass more data to the server if necessary
					}
				}
			};
			TimerTask locationBroadcastTask = new TimerTask()
			{
				@SneakyThrows
				public void run()
				{
					Map<GIMPLocationManager.Coordinate, Integer> location = GIMPLocationManager.mapCoordinates(
							playerLocation.getPlane(),
							playerLocation.getX(),
							playerLocation.getY()
					);
					locationBroadcastManager.broadcast(location);
				}
			};
			timer.schedule(locationPingTask, 0, 5000);
			timer.schedule(locationBroadcastTask, 2500, 5000);
		}
	}

	@Provides
	GIMPConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GIMPConfig.class);
	}
}
