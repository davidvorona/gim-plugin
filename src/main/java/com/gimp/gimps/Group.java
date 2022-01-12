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
package com.gimp.gimps;

import com.gimp.GimPluginConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanID;
import net.runelite.api.clan.ClanMember;
import net.runelite.api.clan.ClanSettings;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import okhttp3.OkHttpClient;

@Slf4j
public class Group
{
	final static int OFFLINE_WORLD = 0;

	@Getter
	final private List<GimPlayer> gimps = new ArrayList<>();

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	public GimPluginConfig config;

	@Inject
	private WorldMapPointManager worldMapPointManager;

	final private HiscoreClient hiscoreClient;

	@Getter
	private boolean loaded = false;

	public Group()
	{
		OkHttpClient okHttpClient = new OkHttpClient();
		hiscoreClient = new HiscoreClient(okHttpClient);
	}

	/**
	 * Loads player data to the Group once the client has finished loading clan
	 * data. Initializes data for the local gimp.
	 *
	 * @return result of loading gimps
	 */
	public CompletableFuture<Void> load()
	{
		CompletableFuture<Void> loadingResult = new CompletableFuture<>();
		clientThread.invokeLater(() ->
		{
			ClanSettings gimClanSettings = client.getClanSettings(ClanID.GROUP_IRONMAN);
			if (gimClanSettings == null)
			{
				// ClanSettings not loaded yet, retry
				return false;
			}
			List<ClanMember> clanMembers = gimClanSettings.getMembers();
			for (ClanMember member : clanMembers)
			{
				String name = member.getName();
				int world = getCurrentWorld(name);
				gimps.add(new GimPlayer(name, world));
			}
			// Load local gimp data, including hiscores
			localLoad().whenCompleteAsync((result, ex) ->
			{
				loaded = true;
				loadingResult.complete(null);
			});
			return true;

		});
		return loadingResult;
	}

	/**
	 * Loads data for the local GimPlayer, including fetching its hiscores.
	 *
	 * @return result of loading local gimp data
	 */
	public CompletableFuture<Void> localLoad()
	{
		CompletableFuture<Void> loadingResult = new CompletableFuture<>();
		Player localPlayer = client.getLocalPlayer();
		GimPlayer localGimp = getLocalGimp();
		// If no local player or gimp, complete load
		if (localPlayer == null || localGimp == null)
		{
			loadingResult.cancel(true);
			return loadingResult;
		}
		localUpdate();
		setHiscores(localGimp.getName()).whenCompleteAsync((result, ext) ->
		{
			loadingResult.complete(null);
		});
		return loadingResult;
	}

	/**
	 * Maps a raw GimPlayer data object to a GimPlayer in the Group.
	 *
	 * @param gimpData a GimPlayer instance holding broadcast data
	 */
	public void update(GimPlayer gimpData)
	{
		for (GimPlayer gimp : gimps)
		{
			String gimpName = gimpData.getName();
			if (gimp.getName().equals(gimpName))
			{
				if (gimpData.getHp() != null)
				{
					gimp.setHp(gimpData.getHp());
				}
				if (gimpData.getMaxHp() != null)
				{
					gimp.setMaxHp(gimpData.getMaxHp());
				}
				if (gimpData.getPrayer() != null)
				{
					gimp.setPrayer(gimpData.getPrayer());
				}
				if (gimpData.getMaxPrayer() != null)
				{
					gimp.setMaxPrayer(gimpData.getMaxPrayer());
				}
				if (gimpData.getCustomStatus() != null)
				{
					gimp.setCustomStatus(gimpData.getCustomStatus());
				}
				// Must set ghost mode before location!
				if (gimpData.getGhostMode() != null)
				{
					setGhostMode(gimpName, gimpData.getGhostMode());
				}
				if (gimpData.getLocation() != null)
				{
					setLocation(gimpName, gimpData.getLocation());
				}
				if (gimpData.getLastActivity() != null)
				{
					gimp.setLastActivity(gimpData.getLastActivity());
				}
			}
		}
	}

	public void unload()
	{
		clearMapPoints();
		gimps.clear();
		loaded = false;
	}

	public GimPlayer getGimp(String name)
	{
		for (GimPlayer gimp : gimps)
		{
			if (gimp.getName().equals(name))
			{
				return gimp;
			}
		}
		return null;
	}

	public GimPlayer getLocalGimp()
	{
		final Player localPlayer = client.getLocalPlayer();
		if (localPlayer != null)
		{
			return getGimp(localPlayer.getName());
		}
		return null;
	}

	/**
	 * Updates the local GimPlayer if that player exists using data
	 * available on the client.
	 */
	public void localUpdate()
	{
		Player localPlayer = client.getLocalPlayer();
		GimPlayer localGimp = getLocalGimp();
		if (localPlayer != null && localGimp != null)
		{
			localGimp.setHp(client.getBoostedSkillLevel(Skill.HITPOINTS));
			localGimp.setMaxHp(client.getRealSkillLevel(Skill.HITPOINTS));
			localGimp.setPrayer(client.getBoostedSkillLevel(Skill.PRAYER));
			localGimp.setMaxPrayer(client.getRealSkillLevel(Skill.PRAYER));
			localGimp.setGhostMode(config.ghostMode());
			setWorld(localGimp.getName(), client.getWorld());
			setLocation(localGimp.getName(), new GimLocation(localPlayer.getWorldLocation()));
		}
	}

	public List<String> getNames()
	{
		List<String> names = new ArrayList<>();
		for (GimPlayer gimp : gimps)
		{
			names.add(gimp.getName());
		}
		return names;
	}

	private void clearMapPoints()
	{
		for (GimPlayer gimp : gimps)
		{
			GimLocation gimLocation = gimp.getLocation();
			if (gimLocation != null)
			{
				WorldMapPoint lastWorldMapPoint = gimLocation.getWorldMapPoint();
				worldMapPointManager.removeIf(x -> x == lastWorldMapPoint);
			}
		}
	}

	/**
	 * Sets a GimPlayer's location using the provided location data and
	 * updates its icon on the world map if the gimp is online and doesn't
	 * have ghost mode enabled.
	 *
	 * @param name     GimPlayer name
	 * @param location GimPlayer location data
	 */
	public void setLocation(String name, GimLocation location)
	{
		GimPlayer gimp = getGimp(name);
		if (gimp == null)
		{
			return;
		}
		// Remove existing world map point
		GimLocation gimLocation = gimp.getLocation();
		if (gimLocation != null)
		{
			WorldMapPoint lastWorldMapPoint = gimLocation.getWorldMapPoint();
			worldMapPointManager.removeIf(x -> x == lastWorldMapPoint);
		}
		// Create new GimLocation from raw data
		GimLocation newGimLocation = new GimLocation(
			location.getX(),
			location.getY(),
			location.getPlane()
		);
		// Set GimPlayer location to new location
		gimp.setLocation(newGimLocation);
		// Add point to world map (if not local player)
		if (gimp != getLocalGimp() && gimp.shouldIncludeLocation() && gimp.getWorld() != OFFLINE_WORLD)
		{
			worldMapPointManager.add(newGimLocation.getWorldMapPoint());
		}
	}

	/**
	 * Sets the world number of the GimPlayer by name and removes
	 * the map point if the world number is 0, e.g. the player is offline.
	 *
	 * @param name  GimPlayer name
	 * @param world world number
	 */
	public void setWorld(String name, int world)
	{
		GimPlayer gimp = getGimp(name);
		if (gimp == null)
		{
			return;
		}
		gimp.setWorld(world);
		// If player is offline, remove map point
		if (world == OFFLINE_WORLD)
		{
			GimLocation gimLocation = gimp.getLocation();
			if (gimLocation != null)
			{
				WorldMapPoint lastWorldMapPoint = gimLocation.getWorldMapPoint();
				worldMapPointManager.removeIf(x -> x == lastWorldMapPoint);
			}
		}
	}

	/**
	 * Sets the ghost mode value of the GimPlayer by name. If the GimPlayer
	 * has a location stored, either removes the corresponding map point or
	 * creates one, depending on the new ghost mode setting.
	 *
	 * @param name      GimPlayer name
	 * @param ghostMode ghost mode setting
	 */
	public void setGhostMode(String name, boolean ghostMode)
	{
		GimPlayer gimp = getGimp(name);
		if (gimp == null)
		{
			return;
		}
		gimp.setGhostMode(ghostMode);
		GimLocation gimLocation = gimp.getLocation();
		if (gimLocation != null)
		{
			// If ghost mode set to true, remove map point
			if (ghostMode)
			{
				WorldMapPoint lastWorldMapPoint = gimLocation.getWorldMapPoint();
				worldMapPointManager.removeIf(x -> x == lastWorldMapPoint);
			}
			// Otherwise, show the map point (if not local player)
			else if (gimp != getLocalGimp())
			{
				worldMapPointManager.add(gimLocation.getWorldMapPoint());
			}
		}
	}

	/**
	 * Gets the world of a player by name, returns 0 if the player
	 * is offline.
	 *
	 * @param name GimPlayer name
	 * @return world number
	 */
	public int getCurrentWorld(String name)
	{
		ClanChannel gimClanChannel = client.getClanChannel(ClanID.GROUP_IRONMAN);
		if (validateGimpName(name) && gimClanChannel != null)
		{
			ClanChannelMember onlineMember = gimClanChannel.findMember(name);
			if (onlineMember != null)
			{
				return onlineMember.getWorld();
			}
		}
		return OFFLINE_WORLD;
	}

	public CompletableFuture<HiscoreResult> setHiscores(String name)
	{
		GimPlayer gimp = getGimp(name);
		return getHiscores(name).whenCompleteAsync((result, ext) ->
		{
			gimp.setHiscores(result);
		});
	}

	public CompletableFuture<HiscoreResult> getHiscores(String name)
	{
		return hiscoreClient.lookupAsync(name, HiscoreEndpoint.NORMAL).whenCompleteAsync((result, ex) ->
		{
			if (result == null || ex != null)
			{
				if (ex != null)
				{
					log.warn("Error fetching Hiscore data " + ex.getMessage());
				}
			}
		});
	}

	private boolean validateGimpName(String name)
	{
		return getGimp(name) != null;
	}
}
