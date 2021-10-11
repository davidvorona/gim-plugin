package com.gimp;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import java.util.*;

@Slf4j
public class GIMPLocationManager
{
    public enum Coordinate
    {
        plane,
        x,
        y
    }

    @Getter(AccessLevel.PACKAGE)
    public Map<String, WorldPoint> gimpLocations;


    public GIMPLocationManager(ArrayList<String> gimpNames)
    {
        gimpLocations = new HashMap<>();
        for(String name : gimpNames)
        {
            gimpLocations.put(name, null);
        }
    }

    static public Map<Coordinate, Integer> mapCoordinates(int plane, int x, int y)
    {
        Map<Coordinate, Integer> location = new HashMap<>();
        location.put(Coordinate.plane, plane);
        location.put(Coordinate.x, x);
        location.put(Coordinate.y, y);
        return location;
    }
}

//				WorldPoint playerLocation = localPlayer.getWorldLocation();
//				worldMapPointManager.removeIf(x -> x == playerWaypoint);
//				playerWaypoint = new WorldMapPoint(playerLocation, PLAYER_ICON);
//				playerWaypoint.setTarget(playerWaypoint.getWorldPoint());
//				worldMapPointManager.add(playerWaypoint);
