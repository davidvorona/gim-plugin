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

import com.gimp.GimPlugin;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for constructing and caching GIM world map icons.
 */
public class GimIconProvider
{
	final static Font FONT = FontManager.getRunescapeBoldFont();

	final private Map<String, BufferedImage> icons;

	public GimIconProvider()
	{
		icons = new HashMap<>(5);
	}

	public BufferedImage getIcon(String name)
	{
		return icons.computeIfAbsent(name, this::generateIcon);
	}

	private BufferedImage generateIcon(String name)
	{
		// Compute some info about username text size
		final Rectangle2D textBounds = FONT.getStringBounds(name,
			new FontRenderContext(null, true, true));
		final int textHeight = (int) textBounds.getHeight();
		final int textWidth = (int) textBounds.getWidth();

		// Load the GIMP logo
		final BufferedImage gimpLogo = ImageUtil.loadImageResource(GimPlugin.class, "gimpoint.png");

		// Initialize the resulting icon, must be large enough to fit everything with the GIMP logo centered
		final BufferedImage icon = new BufferedImage(textWidth + 4,
			gimpLogo.getHeight() + 2 * textHeight,
			BufferedImage.TYPE_INT_ARGB);

		// First, draw the GIMP logo onto the center of the icon
		final Graphics2D graphics = (Graphics2D) icon.getGraphics();
		graphics.drawImage(gimpLogo, (icon.getWidth() - gimpLogo.getWidth()) / 2, (icon.getHeight() - gimpLogo.getHeight()) / 2, null);
		// Then, draw the shadow of the username text (TODO: is there a proper way to do this?)
		final int textX = (icon.getWidth() - textWidth) / 2;
		final int textY = textHeight;
		graphics.setFont(FontManager.getRunescapeBoldFont());
		graphics.setColor(Color.BLACK);
		graphics.drawString(name, textX + 1, textY + 1);

		// Finally, draw the username text centered horizontally above the GIMP logo
		graphics.setColor(Color.WHITE);
		graphics.drawString(name, textX, textY);

		return icon;
	}
}
