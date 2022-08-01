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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

public class GimTabGroup extends JPanel
{
	/* The panel on which the content tab's content will be displayed on. */
	private final JPanel display;
	/* A list of all the tabs contained in this group. */
	private final List<GimTab> tabs = new ArrayList<>();

	public GimTabGroup(JPanel display)
	{
		this.display = display;
		if (display != null)
		{
			this.display.setLayout(new BorderLayout());
		}
		setLayout(new FlowLayout(FlowLayout.CENTER, 8, 0));
		setOpaque(false);
	}

	public GimTabGroup()
	{
		this(null);
	}

	/* Returns the tab on a certain index. */
	public GimTab getTab(int index)
	{

		if (tabs == null || tabs.isEmpty())
		{
			return null;
		}

		return tabs.get(index);
	}

	public GimTab getTab(String gimp)
	{
		if (tabs == null || tabs.isEmpty())
		{
			return null;
		}
		// Get a tab by the associated gimp
		for (GimTab tab : tabs)
		{
			if (tab.getGimp().equals(gimp))
			{
				return tab;
			}
		}
		return null;
	}

	public void addTab(GimTab tab)
	{
		tabs.add(tab);
		add(tab, BorderLayout.NORTH);
	}

	/***
	 * Selects a tab from the group, and sets the display's content to the
	 * tab's associated content.
	 * @param selectedTab - The tab to select
	 */
	public void select(GimTab selectedTab)
	{
		if (!tabs.contains(selectedTab))
		{
			return;
		}

		// If the OnTabSelected returned false, exit the method to prevent tab switching
		if (!selectedTab.select())
		{
			return;
		}

		// If the display is available, switch from the old to the new display
		if (display != null)
		{
			display.removeAll();
			display.revalidate();
			display.repaint();
		}

		// Unselected all other tabs
		for (GimTab tab : tabs)
		{
			if (!tab.equals(selectedTab))
			{
				tab.unselect();
			}
		}

	}
}
