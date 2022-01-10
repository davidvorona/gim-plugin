package com.gimp.gimps;

import com.gimp.GimIconProvider;
import com.gimp.locations.GimLocation;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.awt.image.BufferedImage;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

@Slf4j
public class GimPlayer
{
	final private WorldMapPointManager worldMapPointManager = new WorldMapPointManager();

	@Getter
	final private String name;

	final private BufferedImage icon;

	@Setter
	@Getter
	private Integer hp;

	@Setter
	@Getter
	private Integer maxHp;

	@Setter
	@Getter
	private Integer prayer;

	@Setter
	@Getter
	private Integer maxPrayer;

	@Setter
	@Getter
	private String customStatus;

	@Nullable
	@Getter
	private GimLocation location;

	@Inject
	public GimPlayer(String name)
	{
		this.name = name;
		GimIconProvider iconProvider = new GimIconProvider();
		icon = iconProvider.getIcon(name);
	}

	public void clearMapPoint()
	{
		if (this.location != null)
		{
			WorldMapPoint lastWorldMapPoint = this.location.getWorldMapPoint();
			worldMapPointManager.removeIf(x -> x == lastWorldMapPoint);
		}
	}

	public void setLocation(GimLocation location)
	{
		// Remove existing world map point
		clearMapPoint();
		// Set location to new GimLocation
		this.location = location;
		// Create and configure new world map point
		this.location.setWorldMapPoint(name, icon);
		// Add point to world map
		worldMapPointManager.add(this.location.getWorldMapPoint());
	}

	public Map<String, Object> getData()
	{
		Map<String, Object> data = new HashMap<>();
		data.put("name", name);
		return data;
	}

	public String toJson()
	{
		Map<String, Object> gimpData = new HashMap<>();
		gimpData.put("name", name);
		gimpData.put("hp", hp);
		gimpData.put("maxHp", maxHp);
		gimpData.put("prayer", prayer);
		gimpData.put("maxPrayer", maxPrayer);
		gimpData.put("customStatus", customStatus);
		if (location != null)
		{
			gimpData.put("location", location.getLocation());
		}
		Gson gson = new Gson();
		return gson.toJson(gimpData);
	}
}
