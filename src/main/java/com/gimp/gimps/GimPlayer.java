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
package com.gimp.gimps;

import com.gimp.GimIconProvider;
import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GimPlayer
{
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

	public void setLocation(GimLocation location)
	{
		// Set location to new GimLocation
		this.location = location;
		// Create and configure new world map point
		this.location.setWorldMapPoint(name, icon);
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
