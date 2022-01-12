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
package com.gimp.map;

import com.gimp.gimps.GimPlayer;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/**
 * Wrapper around the built-in {@link WorldMapPointManager}.
 * Primarily adds support for tracking and fetching map points by name.
 */
@Slf4j
public class GimWorldMapPointManager
{
	@Inject
	private WorldMapPointManager _worldMapPointManager;

	private final Map<String, GimWorldMapPoint> points;
	private final GimIconProvider iconProvider;

	public GimWorldMapPointManager()
	{
		points = new HashMap<>();
		iconProvider = new GimIconProvider();
	}

	public GimWorldMapPoint getPoint(String gimpName)
	{
		if (!points.containsKey(gimpName))
		{
			throw new IllegalArgumentException("WorldMapPoint does not exist for user " + gimpName);
		}
		return points.get(gimpName);
	}

	public boolean hasPoint(String gimpName)
	{
		return points.containsKey(gimpName);
	}

	/**
	 * Add the given map point to the world map for the given player.
	 *
	 * @param gimpName      name of the player
	 * @param worldMapPoint world map point to add
	 */
	public void addPoint(String gimpName, GimWorldMapPoint worldMapPoint)
	{
		if (!points.containsKey(gimpName))
		{
			points.put(gimpName, worldMapPoint);
			_worldMapPointManager.add(worldMapPoint.getWorldMapPoint());
			log.info("Add world map point for " + gimpName);
		}
		else
		{
			throw new IllegalStateException("WorldMapPoint for user " + gimpName + " already exists");
		}
	}

	/**
	 * For the given player, create and add a map point to the world map.
	 *
	 * @param gimp player for whom to create and add a map point
	 */
	public void addPoint(GimPlayer gimp)
	{
		final String name = gimp.getName();
		final WorldPoint p = new WorldPoint(gimp.getLocation().getX(),
			gimp.getLocation().getY(),
			gimp.getLocation().getPlane());
		final WorldMapPoint worldMapPoint = new WorldMapPoint(p, iconProvider.getIcon(name));
		// Configure world map point
		worldMapPoint.setTarget(p);
		// Snaps to edge if outside of current map frame
		worldMapPoint.setSnapToEdge(true);
		// Jumps to location if clicked on
		worldMapPoint.setJumpOnClick(true);
		// Name is necessary for jumpOnClick behavior
		worldMapPoint.setName(name);

		addPoint(name, new GimWorldMapPoint(worldMapPoint));
	}

	public void removePoint(String gimpName)
	{
		if (points.containsKey(gimpName))
		{
			_worldMapPointManager.remove(points.get(gimpName).getWorldMapPoint());
			points.remove(gimpName);
			log.info("Remove map point for " + gimpName);
		}
		else
		{
			throw new IllegalStateException("Cannot remove nonexistent WorldMapPoint for user " + gimpName);
		}
	}

	public void clear()
	{
		for (String gimpName : points.keySet())
		{
			removePoint(gimpName);
		}
	}
}
