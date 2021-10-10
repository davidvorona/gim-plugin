package com.gimp;

import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanID;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;

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

	@Getter(AccessLevel.PACKAGE)
	private ArrayList<String> gimps;

	@Getter(AccessLevel.PACKAGE)
	private WorldMapPoint playerWaypoint;

	@Inject
	private WorldMapPointManager worldMapPointManager;

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
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			// https://github.com/iipom/map-waypoints/blob/master/src/main/java/com/iipom/mapwaypoint/MapWaypointPlugin.java
			Player localPlayer = client.getLocalPlayer();
			if (localPlayer != null) {
				WorldPoint playerLocation = localPlayer.getWorldLocation();
				worldMapPointManager.removeIf(x -> x == playerWaypoint);
				playerWaypoint = new WorldMapPoint(playerLocation, PLAYER_ICON);
				playerWaypoint.setTarget(playerWaypoint.getWorldPoint());
				worldMapPointManager.add(playerWaypoint);
			}
		}
	}

	@Subscribe
	public void onClanChannelChanged(ClanChannelChanged clanChannelChanged)
	{
		ClanChannel changedClanChannel = clanChannelChanged.getClanChannel();
		if (changedClanChannel != null)
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
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "You have joined GIM clan " + gimClanChannelName, null);
				}
				else
				{
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "You are in GIM clan " + gimClanChannelName, null);
				}
				List<ClanChannelMember> clanChannelMembers = gimClanChannel.getMembers();
				gimps = new ArrayList<String>();
				for (ClanChannelMember member : clanChannelMembers)
				{
					gimps.add(member.getName());
				}
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Your fellow gimps are: " + gimps, null);
			}
		}
	}

	@Provides
	GIMPConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GIMPConfig.class);
	}
}
