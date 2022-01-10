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

import java.awt.image.BufferedImage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

@Slf4j
public class GimLocation
{
	public enum Coordinate
	{
		plane,
		x,
		y
	}

	@Getter
	final int x;

	@Getter
	final int y;

	@Getter
	final int plane;

	@Getter
	final WorldPoint worldPoint;

	@Getter
	WorldMapPoint worldMapPoint;

	public GimLocation(WorldPoint worldPoint)
	{
		x = worldPoint.getX();
		y = worldPoint.getY();
		plane = worldPoint.getPlane();
		this.worldPoint = worldPoint;
	}

	public GimLocation(int x, int y, int plane)
	{
		this.x = x;
		this.y = y;
		this.plane = plane;
		worldPoint = new WorldPoint(x, y, plane);
	}

	public GimLocation(Map<String, Integer> coordinates)
	{
		x = coordinates.get(Coordinate.x.toString());
		y = coordinates.get(Coordinate.y.toString());
		plane = coordinates.get(Coordinate.plane.toString());
		worldPoint = new WorldPoint(x, y, plane);
	}

	/**
	 * Maps member coordinates x, y, and plane to HashMap.
	 *
	 * @return map: coordinateName => coordinateValue
	 */
	public Map<String, Integer> getLocation()
	{
		Map<String, Integer> location = new HashMap<>();
		location.put(Coordinate.x.toString(), x);
		location.put(Coordinate.y.toString(), y);
		location.put(Coordinate.plane.toString(), plane);
		return location;
	}

	public void setWorldMapPoint(String name, BufferedImage gimpIcon)
	{
		// Set world map point
		worldMapPoint = new WorldMapPoint(worldPoint, gimpIcon);
		// Configure world map point
		worldMapPoint.setTarget(worldPoint);
		// Snaps to edge if outside of current map frame
		worldMapPoint.setSnapToEdge(true);
		// Jumps to location if clicked on
		worldMapPoint.setJumpOnClick(true);
		// Name is necessary for jumpOnClick behavior
		worldMapPoint.setName(name);
	}
}
