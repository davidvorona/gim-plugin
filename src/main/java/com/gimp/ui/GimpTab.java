/*
 * Copyright (c) 2022, David Vorona <davidavorona@gmail.com>
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
package com.gimp.ui;

import com.google.common.base.Strings;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BooleanSupplier;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.ColorScheme;

public class GimpTab extends JLayeredPane
{
	private static final Border SELECTED_BORDER = new CompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.BRAND_ORANGE), BorderFactory.createEmptyBorder(5, 10, 4, 10));

	private static final Border UNSELECTED_BORDER = BorderFactory.createEmptyBorder(5, 10, 5, 10);

	private static final Color ONLINE_COLOR = new Color(0, 146, 54, 230);

	private static final Color OFFLINE_COLOR = Color.RED;

	private static final int STATUS_DOT_SIZE = 5;

	@Getter
	private final String gimp;

	private final JLabel label;

	private final FilledCircle statusDot;

	/* To be executed when the tab is selected */
	@Setter
	private BooleanSupplier onSelectEvent;

	@Getter
	private boolean selected;

	public GimpTab(String string, GimpTabGroup group, String gimp)
	{
		super();

		this.gimp = gimp;

		setLayout(new BorderLayout());

		this.label = new JLabel(string);
		// Pad the left side of the icon to account for the status dot
		this.label.setBorder(new EmptyBorder(0, STATUS_DOT_SIZE, 0, 0));
		add(this.label, BorderLayout.CENTER);
		setLayer(this.label, DEFAULT_LAYER);


		this.statusDot = new FilledCircle(STATUS_DOT_SIZE, STATUS_DOT_SIZE, OFFLINE_COLOR);
		add(this.statusDot, BorderLayout.EAST);
		setLayer(this.statusDot, PALETTE_LAYER);

		unselect();

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				group.select(GimpTab.this);
			}
		});

		if (!Strings.isNullOrEmpty(string))
		{
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent e)
				{
					GimpTab tab = (GimpTab) e.getSource();
					tab.setForeground(Color.WHITE);
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					GimpTab tab = (GimpTab) e.getSource();
					if (!tab.isSelected())
					{
						tab.setForeground(Color.GRAY);
					}
				}
			});
		}
	}

	public GimpTab(ImageIcon icon, GimpTabGroup group, String gimp)
	{
		this("", group, gimp);
		setOpaque(true);
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		this.drawLabel(icon);

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				GimpTab tab = (GimpTab) e.getSource();
				tab.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				GimpTab tab = (GimpTab) e.getSource();
				tab.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		});

	}

	private void drawLabel(ImageIcon icon)
	{
		this.label.setIcon(icon);
		this.label.setOpaque(false);
		this.label.setVerticalAlignment(SwingConstants.CENTER);
		this.label.setHorizontalAlignment(SwingConstants.CENTER);
	}

	public void setStatus(boolean isOnline)
	{
		Color color = isOnline ? ONLINE_COLOR : OFFLINE_COLOR;
		this.statusDot.setColor(color);
		this.statusDot.repaint();
	}

	public boolean select()
	{
		if (onSelectEvent != null)
		{
			if (!onSelectEvent.getAsBoolean())
			{
				return false;
			}
		}

		setBorder(SELECTED_BORDER);
		setForeground(Color.WHITE);
		return selected = true;
	}

	public void unselect()
	{
		setBorder(UNSELECTED_BORDER);
		setForeground(Color.GRAY);
		selected = false;
	}

	@Override
	public void setForeground(Color fg)
	{
		super.setForeground(fg);
	}

	@Override
	public void setBackground(Color bg)
	{
		super.setBackground(bg);
	}
}
