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
package com.gimp;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import java.util.*;

@Slf4j
public class GIMPLocationManager
{
	@Getter(AccessLevel.PACKAGE)
	public Map<String, GIMPLocation> gimpLocations;

	public GIMPLocationManager(ArrayList<String> gimpNames)
	{
		gimpLocations = new HashMap<>();
		for (String name : gimpNames)
		{
			gimpLocations.put(name, null);
		}
	}

	/**
	 * Save each location to gimpLocations Map.
	 *
	 * @param data map: name => location
	 */
	public void update(Map<String, GIMPLocation> data)
	{
		gimpLocations.clear();
		for (String name : data.keySet())
		{
			GIMPLocation location = data.get(name);
			gimpLocations.put(name, location);
		}
	}

	/**
	 * Get WorldPoint instances from locations.
	 * TODO: Probably want to move this to its own class that handles world points.
	 *
	 * @return map: name => worldPoint
	 */
	public Map<String, WorldPoint> getGimpWorldPoints()
	{
		Map<String, WorldPoint> worldPoints = new HashMap<>();
		for (String name : gimpLocations.keySet())
		{
			GIMPLocation location = gimpLocations.get(name);
			WorldPoint worldPoint = new WorldPoint(
				location.getX(),
				location.getY(),
				location.getPlane()
			);
			worldPoints.put(name, worldPoint);
		}
		return worldPoints;
	}
}
