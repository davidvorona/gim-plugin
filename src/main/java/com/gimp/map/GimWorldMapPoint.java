package com.gimp.map;

import com.gimp.gimps.GimLocation;
import com.gimp.gimps.GimPlayer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

/**
 * Wrapper around the built-in {@link WorldMapPoint}.
 * Creates abstraction for "moving" the world map point by altering the underlying map point location.
 *
 * Supports fractional locations which allows for map points to be moved more slowly.
 * TODO: The current implementation doesn't actually need this due to the "frame toggle" approach; delete when sure.
 */
public class GimWorldMapPoint
{
	private final WorldMapPoint worldMapPoint;
	private double x;
	private double y;

	public GimWorldMapPoint(WorldMapPoint worldMapPoint)
	{
		this.worldMapPoint = worldMapPoint;
		x = this.worldMapPoint.getWorldPoint().getX();
		y = this.worldMapPoint.getWorldPoint().getY();
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

	public void moveTowardPlayer(GimPlayer gimp, boolean frameToggle)
	{
		final String name = gimp.getName();
		final WorldPoint shownLocation = getWorldPoint();
		final GimLocation targetLocation = gimp.getLocation();
		if (shownLocation != null && targetLocation != null)
		{
			int dx = targetLocation.getX() - shownLocation.getX();
			int dy = targetLocation.getY() - shownLocation.getY();
			if (targetLocation.getPlane() != shownLocation.getPlane()
				|| Math.abs(dx) > 30
				|| Math.abs(dy) > 30)
			{
				// If the target location is too far (or in another plane), instantly change the map point
				setWorldPoint(targetLocation.toWorldPoint());
			}
			else if (dx != 0 || dy != 0)
			{
				// Otherwise if it's moved at all, smoothly move it toward the target location.
				// If moving fast (e.g. running), move every frame; otherwise, move every other frame.
				if (frameToggle || gimp.getSpeed() > 2.25)
				{
					// Only move by a max of 1 tile in a given axis (keeps it consistent/smooth)
					move(Math.min(1, Math.max(dx, -1)), Math.min(1, Math.max(dy, -1)));
				}
			}
		}
	}
}
