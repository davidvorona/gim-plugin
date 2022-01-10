package com.gimp.gimps;

import com.gimp.locations.GimLocation;
import java.util.ArrayList;
import java.util.List;
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
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

@Slf4j
public class Group
{
	@Getter
	final private List<GimPlayer> gimps = new ArrayList<>();

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private WorldMapPointManager worldMapPointManager;

	@Getter
	private boolean loaded = false;

	public void load()
	{
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
				gimps.add(new GimPlayer(member.getName()));
			}
			initLocalGimp();
			loaded = true;
			return true;
		});
	}

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
				if (gimpData.getLocation() != null)
				{
					setLocation(gimpName, gimpData.getLocation());
				}
			}
		}
	}

	public void waitForLoad(Runnable r)
	{
		clientThread.invokeLater(() ->
		{
			if (!isLoaded())
			{
				return false;
			}
			r.run();
			return true;
		});
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

	public void initLocalGimp()
	{
		Player localPlayer = client.getLocalPlayer();
		GimPlayer localGimp = getLocalGimp();
		if (localPlayer != null && localGimp != null)
		{
			localGimp.setHp(client.getBoostedSkillLevel(Skill.HITPOINTS));
			localGimp.setMaxHp(client.getRealSkillLevel(Skill.HITPOINTS));
			localGimp.setPrayer(client.getBoostedSkillLevel(Skill.PRAYER));
			localGimp.setMaxPrayer(client.getRealSkillLevel(Skill.PRAYER));
			GimLocation location = new GimLocation(localPlayer.getWorldLocation());
			localGimp.setLocation(location);
		}
	}

	public int getIndexOfGimp(String name)
	{
		for (GimPlayer gimp : gimps)
		{
			if (gimp.getName().equals(name))
			{
				return gimps.indexOf(gimp);
			}
		}
		return -1;
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
		if (gimp != getLocalGimp())
		{
			worldMapPointManager.add(newGimLocation.getWorldMapPoint());
		}
	}

	public int getWorld(String name)
	{
		int OFFLINE_WORLD = 0;
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

	private boolean validateGimpName(String name)
	{
		return getGimp(name) != null;
	}
}
