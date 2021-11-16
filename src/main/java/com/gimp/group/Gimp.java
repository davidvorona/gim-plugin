package com.gimp.group;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

@Slf4j
public class Gimp
{
	@Getter
	final private String name;

	@Setter
	@Getter
	private int currentHp;

	@Setter
	@Getter
	private int maxHp;

	@Setter
	@Getter
	private int currentPrayer;

	@Setter
	@Getter
	private int maxPrayer;

	@Setter
	@Getter
	private String customStatus;

	@Setter
	@Getter
	private WorldPoint worldPoint;

	Gimp(String name)
	{
		this.name = name;
	}
}
