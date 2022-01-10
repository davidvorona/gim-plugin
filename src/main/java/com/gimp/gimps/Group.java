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

@Slf4j
public class Group
{
	@Getter
	final private List<GimPlayer> gimps = new ArrayList<>();

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

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
		log.debug(gimpData.toJson());
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
					gimp.setLocation(gimpData.getLocation());
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
			gimp.clearMapPoint();
		}
	}

	public void setLocation(String name, GimLocation location)
	{
		GimPlayer gimp = getGimp(name);
		if (gimp == null)
		{
			return;
		}
		gimp.setLocation(location);
	}

	public void setCurrentHp(String name, int hp)
	{
		GimPlayer gimp = getGimp(name);
		if (gimp == null)
		{
			return;
		}
		gimp.setHp(hp);
	}

	public void setMaxHp(String name, int hpMax)
	{
		GimPlayer gimp = getGimp(name);
		if (gimp == null)
		{
			return;
		}
		gimp.setMaxHp(hpMax);
	}

	public void setCurrentPrayer(String name, int prayer)
	{
		GimPlayer gimp = getGimp(name);
		if (gimp == null)
		{
			return;
		}
		gimp.setPrayer(prayer);
	}

	public void setMaxPrayer(String name, int prayerMax)
	{
		GimPlayer gimp = getGimp(name);
		if (gimp == null)
		{
			return;
		}
		gimp.setMaxPrayer(prayerMax);
	}

	public void setCustomStatus(String name, String customStatus)
	{
		GimPlayer gimp = getGimp(name);
		if (gimp == null)
		{
			return;
		}
		gimp.setCustomStatus(customStatus);
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
