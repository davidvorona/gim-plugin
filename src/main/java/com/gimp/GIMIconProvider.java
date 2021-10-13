package com.gimp;

import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class GIMIconProvider
{
	final static Font FONT = FontManager.getRunescapeBoldFont();

	private Map<String, BufferedImage> icons;

	public GIMIconProvider()
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
		final BufferedImage gimpLogo = ImageUtil.loadImageResource(GIMPlugin.class, "gimpoint.png");

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
