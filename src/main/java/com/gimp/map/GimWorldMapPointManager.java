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
