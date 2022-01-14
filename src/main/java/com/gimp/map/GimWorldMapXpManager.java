package com.gimp.map;

import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
public class GimWorldMapXpManager
{
	private static final int OFFSET = 5;

	@Inject
	private WorldMapPointManager worldMapPointManager;

	public void spawnOn(WorldPoint worldPoint, Map<Skill, Integer> xpDiff)
	{
		final WorldPoint p = getOffsetWorldPoint(worldPoint);
		// XP diff should only include 1 skill
		Map.Entry<Skill, Integer> entry = xpDiff.entrySet().iterator().next();
		Skill skill = entry.getKey();
		final WorldMapPoint worldMapPoint = new WorldMapPoint(p, getSkillIcon(skill));
		// Set image point to map point origin
		worldMapPoint.setImagePoint(new Point(0, 0));
		// TODO: Add point to map, move map point up, then despawn
		// worldMapPointManager.add(worldMapPoint);
	}

	private BufferedImage getSkillIcon(Skill skill)
	{
		String iconPath = "/skill_icons_small/" + skill.toString().toLowerCase(Locale.ROOT) + ".png";
		return ImageUtil.loadImageResource(getClass(), iconPath);
	}

	private WorldPoint getOffsetWorldPoint(WorldPoint worldPoint)
	{
		return new WorldPoint(
			worldPoint.getX() + OFFSET,
			worldPoint.getY() + OFFSET,
			worldPoint.getPlane()
		);
	}
}
