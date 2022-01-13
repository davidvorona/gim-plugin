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

import java.awt.Color;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.hiscore.HiscoreResult;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GimPlayer
{
	@Getter
	final private String name;

	@Setter
	@Getter
	private Integer hp;

	@Setter
	@Getter
	private Integer maxHp;

	@Setter
	@Getter
	private Integer prayer;

	@Setter
	@Getter
	private Integer maxPrayer;

	@Setter
	@Getter
	private HiscoreResult hiscores;

	@Setter
	@Getter
	private String customStatus;

	@Setter
	@Getter
	private Integer world;

	@Nullable
	@Getter
	private GimLocation location;

	@Setter
	@Getter
	private Boolean ghostMode;

	@Setter
	@Getter
	private String lastActivity = IN_GAME_ACTIVITY;

	public static final String IN_GAME_ACTIVITY = "IN_GAME_ACTIVITY";

	/**
	 * Speed of this player in tiles per second.
	 */
	@Getter
	private double speed;

	/**
	 * Timestamp of the last location update (in system time milliseconds).
	 * Used for computing the effective "speed" of the player.
	 */
	private long locationTimestamp;

	@Getter
	private final Color color;

	@Inject
	public GimPlayer(String name, int world, Color color)
	{
		this.name = name;
		this.world = world;
		this.color = color;
	}

	public void setLocation(GimLocation location)
	{
		// Determine the "speed" of the player
		if (this.location != null)
		{
			final long millisSinceLastLocation = System.currentTimeMillis() - locationTimestamp;
			final double distance = this.location.getDistanceTo(location);
			if (this.location.plane != location.plane)
			{
				// If switching between planes, keep the speed at zero
				speed = 0;
			}
			else
			{
				// Otherwise, compute the speed (doesn't account for teleports, but oh well...)
				speed = distance * 1000.0 / millisSinceLastLocation;
			}
		}
		// Set location to new GimLocation
		this.location = location;
		// Update timestamp
		locationTimestamp = System.currentTimeMillis();
	}

	public Map<String, Object> getData()
	{
		Map<String, Object> data = new HashMap<>();
		data.put("name", name);
		return data;
	}

	public boolean shouldIncludeLocation()
	{
		return ghostMode == null || !ghostMode;
	}

	/**
	 * Retrieves GimPlayer data for a broadcast. Suppresses and/or
	 * modifies data for this purpose.
	 *
	 * @return GimPlayer broadcast-ready data
	 */
	public Map<String, Object> getGimpData()
	{
		Map<String, Object> gimpData = new HashMap<>();
		gimpData.put("name", name);
		gimpData.put("hp", hp);
		gimpData.put("maxHp", maxHp);
		gimpData.put("prayer", prayer);
		gimpData.put("maxPrayer", maxPrayer);
		gimpData.put("customStatus", customStatus);
		gimpData.put("ghostMode", ghostMode);
		// Don't get location if ghostMode is active
		if (location != null && this.shouldIncludeLocation())
		{
			gimpData.put("location", location.getLocation());
		}
		gimpData.put("lastActivity", lastActivity);
		return gimpData;
	}
}
