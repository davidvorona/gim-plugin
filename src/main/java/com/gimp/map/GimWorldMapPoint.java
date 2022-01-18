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

import com.gimp.gimps.GimLocation;
import com.gimp.gimps.GimPlayer;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

/**
 * Wrapper around the built-in {@link WorldMapPoint}.
 * Creates abstraction for "moving" the world map point by altering the underlying map point location.
 * <p>
 * Supports fractional locations which allows for map points to be moved more slowly.
 * TODO: The current implementation doesn't actually need this due to the "frame toggle" approach; delete when sure.
 */
public class GimWorldMapPoint
{
	private final GimPlayer gimp;
	private final WorldMapPoint worldMapPoint;
	private double x;
	private double y;

	private final Queue<WorldMapPoint> footsteps;
	private final BufferedImage mmIcon;

	public GimWorldMapPoint(GimPlayer gimp, WorldMapPoint worldMapPoint)
	{
		this.gimp = gimp;
		this.worldMapPoint = worldMapPoint;
		x = this.worldMapPoint.getWorldPoint().getX();
		y = this.worldMapPoint.getWorldPoint().getY();
		footsteps = new LinkedList<>();

		// Initialize the footstep icon to be used
		final BufferedImage icon = new BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = icon.createGraphics();
		g.setColor(gimp.getColor());
		g.fillOval(0, 0, 12, 12);
		mmIcon = icon;

	}

	public void setX(double x)
	{
		this.x = x;
		refreshWorldPoint();
	}

	public void setY(double y)
	{
		this.y = y;
		refreshWorldPoint();
	}

	public void move(double dx, double dy)
	{
		x += dx;
		y += dy;
		refreshWorldPoint();
	}

	public void setWorldPoint(WorldPoint worldPoint)
	{
		worldMapPoint.setWorldPoint(worldPoint);
		x = worldPoint.getX();
		y = worldPoint.getY();
	}

	public WorldPoint getWorldPoint()
	{
		return worldMapPoint.getWorldPoint();
	}

	public WorldMapPoint getWorldMapPoint()
	{
		return worldMapPoint;
	}

	private void refreshWorldPoint()
	{
		if ((int) x != worldMapPoint.getWorldPoint().getX()
			|| (int) y != worldMapPoint.getWorldPoint().getY())
		{
			final WorldPoint newWorldPoint = new WorldPoint((int) x, (int) y, worldMapPoint.getWorldPoint().getPlane());
			worldMapPoint.setWorldPoint(newWorldPoint);
		}
	}

	public boolean moveTowardPlayer(boolean frameToggle)
	{
		final WorldPoint shownLocation = getWorldPoint();
		final GimLocation targetLocation = gimp.getLocation();
		if (shownLocation != null && targetLocation != null)
		{
			int dx = targetLocation.getX() - shownLocation.getX();
			int dy = targetLocation.getY() - shownLocation.getY();
			if (Math.abs(dx) > 30 || Math.abs(dy) > 30)
			{
				// If the target location is too far, instantly change the map point
				setWorldPoint(targetLocation.toWorldPoint());
				return true;
			}
			else if (dx != 0 || dy != 0)
			{
				// Otherwise if it's moved at all, smoothly move it toward the target location.
				// If moving fast (e.g. running), move every frame; otherwise, move every other frame.
				if (frameToggle || gimp.getSpeed() > 2.25)
				{
					// Only move by a max of 1 tile in a given axis (keeps it consistent/smooth)
					move(Math.min(1, Math.max(dx, -1)), Math.min(1, Math.max(dy, -1)));
					// If the plane has changed, update it while preserving the shown x/y coordinates
					if (shownLocation.getPlane() != targetLocation.getPlane())
					{
						setWorldPoint(new WorldPoint(shownLocation.getX(), shownLocation.getY(), targetLocation.getPlane()));
					}
					return true;
				}
			}
		}
		return false;
	}

	public void addFootstep(GimWorldMapPointManager gimWorldMapPointManager, int maxLength)
	{
		final WorldPoint worldPoint = worldMapPoint.getWorldPoint().dx(0);

		if (maxLength > 0)
		{
			// Optimization to reuse the tail WMP as the head WMP (since altering the underlying WMP ArrayList is expensive)
			if (footsteps.size() == maxLength)
			{
				final WorldMapPoint popped = footsteps.remove();
				popped.setWorldPoint(worldPoint);
				footsteps.add(popped);
				return;
			}

			final WorldMapPoint worldMapPoint = new WorldMapPoint(worldPoint, mmIcon);
			gimWorldMapPointManager.addAssociatedPoint(gimp.getName(), worldMapPoint);
			footsteps.add(worldMapPoint);
		}

		while (footsteps.size() > maxLength)
		{
			final WorldMapPoint popped = footsteps.remove();
			gimWorldMapPointManager.removeAssociatedPoint(gimp.getName(), popped);
		}

	}
}
