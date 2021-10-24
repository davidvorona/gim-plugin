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
package com.gimp.locations;

import com.gimp.GIMPIconProvider;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import java.util.*;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

@Slf4j
public class GIMPLocationManager
{
	@Inject
	private GIMPIconProvider iconProvider;

	@Inject
	private WorldMapPointManager worldMapPointManager;

	@Getter(AccessLevel.PACKAGE)
	final private Map<String, GIMPLocation> gimpLocations = new HashMap<>();

	@Getter(AccessLevel.PACKAGE)
	final private List<WorldMapPoint> playerWaypoints = new ArrayList<>();

	/**
	 * Saves each location to gimpLocations Map.
	 *
	 * @param data map: name => GIMPLocation
	 */
	public void updateLocations(Map<String, GIMPLocation> data)
	{
		gimpLocations.clear();
		for (String name : data.keySet())
		{
			GIMPLocation location = data.get(name);
			gimpLocations.put(name, location);
		}
	}

	public void updateMapPoints(Player localPlayer)
	{
		Map<String, WorldPoint> gimpWorldPoints = getOtherGimpWorldPoints(localPlayer.getName());
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

	/**
	 * Gets the WorldPoint of the local player and returns a GIMP
	 * location with coordinates x, y, and plane.
	 *
	 * @param localPlayer client's local player instance
	 * @return GIMPLocation of local player
	 */
	public GIMPLocation getCurrentLocation(Player localPlayer)
	{
		WorldPoint worldPoint = localPlayer.getWorldLocation();
		return new GIMPLocation(
			worldPoint.getX(),
			worldPoint.getY(),
			worldPoint.getPlane()
		);
	}

	/**
	 * Gets WorldPoint instances from locations, excluding local player.
	 *
	 * @return map: name => worldPoint
	 */
	public Map<String, WorldPoint> getOtherGimpWorldPoints(String localName)
	{
		Map<String, WorldPoint> worldPoints = new HashMap<>();
		for (String name : gimpLocations.keySet())
		{
			if (!name.equals(localName))
			{
				GIMPLocation location = gimpLocations.get(name);
				worldPoints.put(name, location.getWorldPoint());
			}
		}
		return worldPoints;
	}
}
