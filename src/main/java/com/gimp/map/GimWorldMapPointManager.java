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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
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
	private final Map<String, Collection<WorldMapPoint>> associatedPoints;
	private final GimIconProvider iconProvider;

	public GimWorldMapPointManager()
	{
		points = new HashMap<>();
		associatedPoints = new HashMap<>();
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
			associatedPoints.put(gimpName, new LinkedList<>());
			_worldMapPointManager.add(worldMapPoint.getWorldMapPoint());
			log.debug("Add world map point for " + gimpName);
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
		if (gimp.getLocation() != null)
		{
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

<<<<<<< HEAD
		addPoint(name, new GimWorldMapPoint(gimp, worldMapPoint));
=======
			addPoint(name, new GimWorldMapPoint(worldMapPoint));
		}
>>>>>>> Add skill XP updates and processing
	}

	public void removePoint(String gimpName)
	{
		if (points.containsKey(gimpName))
		{
			// Remove the user's primary WMP
			_worldMapPointManager.remove(points.get(gimpName).getWorldMapPoint());
			points.remove(gimpName);

			// Remove the user's associated WMPs
			for (WorldMapPoint point : associatedPoints.get(gimpName))
			{
				_worldMapPointManager.removeIf(wmp -> wmp == point);
			}
			associatedPoints.remove(gimpName);

			log.debug("Remove map point for " + gimpName);
		}
		else
		{
			throw new IllegalStateException("Cannot remove nonexistent WorldMapPoint for user " + gimpName);
		}
	}

	public void addAssociatedPoint(String gimpName, WorldMapPoint associatedPoint)
	{
		associatedPoints.get(gimpName).add(associatedPoint);
		_worldMapPointManager.add(associatedPoint);
		// Re-add the gimp's WMP to ensure it's rendered on top (this might be expensive, so avoid if possible)
		_worldMapPointManager.remove(points.get(gimpName).getWorldMapPoint());
		_worldMapPointManager.add(points.get(gimpName).getWorldMapPoint());
	}

	public void removeAssociatedPoint(String gimpName, WorldMapPoint worldMapPoint)
	{
		associatedPoints.get(gimpName).removeIf(wmp -> wmp == worldMapPoint);
		_worldMapPointManager.removeIf(wmp -> wmp == worldMapPoint);
	}

	public void clear()
	{
		// Use key collection rather than iterator to avoid concurrent modification
		final Collection<String> gimpNames = new ArrayList<>(points.keySet());

		for (String gimpName : gimpNames)
		{
			if (hasPoint(gimpName))
			{
				removePoint(gimpName);
			}
		}
	}
}
