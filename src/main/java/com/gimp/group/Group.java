package com.gimp.group;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanID;
import net.runelite.api.clan.ClanMember;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;

@Slf4j
public class Group
{
	@Getter
	final private List<Gimp> gimps = new ArrayList<>();

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
				gimps.add(new Gimp(member.getName()));
			}
			loaded = true;
			return true;
		});
	}

	public void unload()
	{
		gimps.clear();
		loaded = false;
	}

	public Gimp getGimp(String name)
	{
		for (Gimp gimp : gimps)
		{
			if (gimp.getName().equals(name))
			{
				return gimp;
			}
		}
		return null;
	}

	public Gimp getLocalGimp()
	{
		final Player localPlayer = client.getLocalPlayer();
		if (localPlayer != null)
		{
			return getGimp(localPlayer.getName());
		}
		return null;
	}

	public int getIndexOfGimp(String name)
	{
		for (Gimp gimp : gimps)
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
		for (Gimp gimp : gimps)
		{
			names.add(gimp.getName());
		}
		return names;
	}

	public void setWorldPoint(String name, WorldPoint worldPoint)
	{
		Gimp gimp = getGimp(name);
		if (gimp == null)
		{
			return;
		}
		gimp.setWorldPoint(worldPoint);
	}

	public void setCurrentHp(String name, int hp)
	{
		Gimp gimp = getGimp(name);
		if (gimp == null)
		{
			return;
		}
		gimp.setCurrentHp(hp);
	}

	public void setMaxHp(String name, int hpMax)
	{
		Gimp gimp = getGimp(name);
		if (gimp == null)
		{
			return;
		}
		gimp.setMaxHp(hpMax);
	}

	public void setCurrentPrayer(String name, int prayer)
	{
		Gimp gimp = getGimp(name);
		if (gimp == null)
		{
			return;
		}
		gimp.setCurrentPrayer(prayer);
	}

	public void setMaxPrayer(String name, int prayerMax)
	{
		Gimp gimp = getGimp(name);
		if (gimp == null)
		{
			return;
		}
		gimp.setMaxPrayer(prayerMax);
	}

	public void setCustomStatus(String name, String customStatus)
	{
		Gimp gimp = getGimp(name);
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
