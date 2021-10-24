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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

@Slf4j
public class GIMPLocation
{
	public enum Coordinate
	{
		plane,
		x,
		y
	}

	@Getter(AccessLevel.PACKAGE)
	int x;

	@Getter(AccessLevel.PACKAGE)
	int y;

	@Getter(AccessLevel.PACKAGE)
	int plane;

	public GIMPLocation(int xArg, int yArg, int planeArg)
	{
		x = xArg;
		y = yArg;
		plane = planeArg;
	}

	/**
	 * Maps member coordinates x, y, and plane to HashMap.
	 *
	 * @return map: coordinateName => coordinateValue
	 */
	public Map<String, Object> getLocation()
	{
		Map<String, Object> location = new HashMap<>();
		location.put(Coordinate.x.toString(), x);
		location.put(Coordinate.y.toString(), y);
		location.put(Coordinate.plane.toString(), plane);
		return location;
	}

	/**
	 * Gets WorldPoint instance from member coordinates.
	 *
	 * @return WorldPoint from coordinates
	 */
	public WorldPoint getWorldPoint()
	{
		return new WorldPoint(x, y, plane);
	}
}
