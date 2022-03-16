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
package com.gimp;

import com.gimp.gimps.*;
import com.google.common.collect.ImmutableList;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanID;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.ui.components.ProgressBar;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.client.hiscore.HiscoreSkill;
import static net.runelite.client.hiscore.HiscoreSkill.*;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkillType;
import net.runelite.client.hiscore.Skill;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class GimPluginPanel extends PluginPanel
{
	private static final ImageIcon GIMP_ICON_SMALL;

	/**
	 * Real skills, ordered in the way they should be displayed in the panel.
	 */
	private static final List<HiscoreSkill> SKILLS = ImmutableList.of(
		ATTACK, HITPOINTS, MINING,
		STRENGTH, AGILITY, SMITHING,
		DEFENCE, HERBLORE, FISHING,
		RANGED, THIEVING, COOKING,
		PRAYER, CRAFTING, FIREMAKING,
		MAGIC, FLETCHING, WOODCUTTING,
		RUNECRAFT, SLAYER, FARMING,
		CONSTRUCTION, HUNTER
	);

	private static final String HTML_LABEL_TEMPLATE =
		"<html><body style='color:%s'>%s<span style='color:white'>%s</span></body></html>";

	private final Group group;

	@Inject
	private Client client;

	// Not an EnumMap because we need null keys for combat
	private final Map<HiscoreSkill, JLabel> skillLabels = new HashMap<>();

	private static final Color HP_FG = new Color(0, 146, 54, 230);
	private static final Color HP_BG = new Color(102, 15, 16, 230);
	private static final Color PRAYER_FG = new Color(0, 149, 151);
	private static final Color PRAYER_BG = Color.black;

	private static final String CONNECTING_TEXT = "Trying to connect...";

	private final JLabel noDataLabel = new JLabel();
	private final JLabel connectionLabel = new JLabel();
	private final JLabel usernameLabel = new JLabel();
	private final JLabel worldLabel = new JLabel();
	private final ProgressBar hpBar = new ProgressBar();
	private final ProgressBar prayerBar = new ProgressBar();
	private final JButton refreshButton = new JButton("Refresh");
	private final JLabel activityLabel = new JLabel();

	/* Container of all the selectable gimp tabs */
	private MaterialTabGroup tabGroup;

	/* Index of the local gimp's tab or 0 */
	private int defaultTab;

	/* The currently selected gimp */
	private String selectedGimp;

	/* Used to prevent users from switching gimp tabs while the results are loading */
	private boolean loading = false;

	static
	{
		final BufferedImage gimpIconSmallImg = ImageUtil.loadImageResource(GimPluginPanel.class, "gimpoint-small.png");
		GIMP_ICON_SMALL = new ImageIcon(gimpIconSmallImg);
	}

	@Inject
	public GimPluginPanel(GimPlugin plugin)
	{
		this.group = plugin.getGroup();

		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout());
	}

	public void load()
	{
		final Player localPlayer = client.getLocalPlayer();
		List<String> gimps = group.getNames();
		try
		{
			SwingUtilities.invokeLater(() ->
			{
				// Remove noData text
				removeAll();
				// Create panel that will hold gimp data
				final JPanel container = new JPanel();
				container.setBackground(ColorScheme.DARK_GRAY_COLOR);
				container.setLayout(new GridBagLayout());

				// Expand sub items to fit width of panel, align to top of panel
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.HORIZONTAL;
				c.gridx = 0;
				c.gridy = 0;
				c.weightx = 1;
				c.weighty = 0;
				c.insets = new Insets(0, 0, 10, 0);

				// Add connection status panel
				final JPanel statusPanel = makeStatusPanel();
				container.add(statusPanel, c);
				c.gridy++;

				// Add tabs for each gimp
				int gimpCount = gimps.size();
				tabGroup = new MaterialTabGroup();
				tabGroup.setLayout(new GridLayout(1, gimpCount, 7, 7));

				int tabIdx = 0;
				for (String username : gimps)
				{
					MaterialTab tab = new MaterialTab(GIMP_ICON_SMALL, tabGroup, null);
					tab.setToolTipText(username);
					tab.setOnSelectEvent(() ->
					{
						if (loading)
						{
							return false;
						}

						selectedGimp = username;
						// Removes focus border from refresh button on tab select
						tab.requestFocus();
						return true;
					});
					// Adding the lookup method to a mouseListener instead of the above onSelectedEvent
					// because sometimes you might want to switch the tab, without calling for lookup
					tab.addMouseListener(new MouseAdapter()
					{
						@Override
						public void mousePressed(MouseEvent mouseEvent)
						{
							if (loading)
							{
								return;
							}
							loadGimpData();
						}
					});
					// Set tab of local gimp, if none is defined yet will default to first
					if (localPlayer != null && username.equals(localPlayer.getName()))
					{
						defaultTab = tabIdx;
					}
					tabGroup.addTab(tab);
					tabIdx++;
				}

				// Default selected tab is you
				resetSelectedTab();

				container.add(tabGroup, c);
				c.gridy++;

				// Panel that hold gimp info and status
				JPanel infoPanel = makeInfoPanel();
				container.add(infoPanel, c);
				c.gridy++;

				// Panel that holds skill icons
				JPanel statsPanel = new JPanel();
				statsPanel.setLayout(new GridLayout(8, 3));
				statsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				statsPanel.setBorder(new EmptyBorder(5, 0, 5, 0));

				// For each skill on the in-game panel, create a Label and add it to the UI
				for (HiscoreSkill skill : SKILLS)
				{
					JPanel panel = makeHiscorePanel(skill);
					statsPanel.add(panel);
				}

				container.add(statsPanel, c);
				c.gridy++;

				// Create button to refresh gimp data
				refreshButton.addActionListener((e) ->
				{
					loadGimpData();
				});
				container.add(refreshButton, c);
				c.gridy++;

				// Add data container to panel
				add(container, BorderLayout.CENTER);
				// Revalidate layout and repaint
				revalidate();
				repaint();

				// Load gimp data into panel
				loadGimpData();
			});
		}
		catch (Exception e)
		{
			log.error(e.toString());
		}
	}

	public void unload()
	{
		SwingUtilities.invokeLater(() ->
		{
			// Remove GIMP data
			removeAll();
			// Create noData panel
			JPanel noDataPanel = makeNoDataPanel();
			add(noDataPanel, BorderLayout.CENTER);
			// Revalidate layout and repaint
			revalidate();
			repaint();
		});
	}

	private JPanel makeNoDataPanel()
	{
		assert SwingUtilities.isEventDispatchThread();

		JPanel container = new JPanel();
		container.setLayout(new BorderLayout());
		noDataLabel.setFont(FontManager.getRunescapeFont());
		noDataLabel.setText("<html><body style='text-align:center;'>You must be logged in to a group ironman to see GIMP data.</body></html>");
		container.add(noDataLabel);
		return container;
	}

	private JPanel makeStatusPanel()
	{
		assert SwingUtilities.isEventDispatchThread();

		// Create a panel to hold plugin status info
		final JPanel statusPanel = new JPanel();
		statusPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		statusPanel.setLayout(new DynamicGridLayout(1, 1, 0, 0));
		statusPanel.setBorder(new EmptyBorder(2, 8, 2, 8));
		// Add connection status
		connectionLabel.setHorizontalAlignment(SwingConstants.CENTER);
		connectionLabel.setFont(FontManager.getRunescapeFont());
		connectionLabel.setText(CONNECTING_TEXT);
		statusPanel.add(connectionLabel);

		return statusPanel;
	}

	private JPanel makeInfoPanel()
	{
		assert SwingUtilities.isEventDispatchThread();

		// Create panel that will contain overall data
		JPanel overallPanel = new JPanel();
		overallPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(5, 0, 0, 0, ColorScheme.DARK_GRAY_COLOR),
			BorderFactory.createEmptyBorder(8, 10, 8, 10)
		));
		overallPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		overallPanel.setLayout(new BorderLayout());

		// Add icon and contents
		final JPanel overallInfo = new JPanel();
		overallInfo.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		overallInfo.setLayout(new DynamicGridLayout(3, 1, 0, 4));
		overallInfo.setBorder(new EmptyBorder(2, 10, 2, 10));

		// Add title panel
		JPanel titleWrapper = new JPanel();
		titleWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		titleWrapper.setLayout(new DynamicGridLayout(1, 2, 0, 0));
		titleWrapper.setBorder(new EmptyBorder(2, 0, 0, 0));
		// Add username label
		usernameLabel.setHorizontalAlignment(SwingConstants.LEFT);
		usernameLabel.setFont(FontManager.getRunescapeBoldFont());
		titleWrapper.add(usernameLabel);
		// Add world label
		worldLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		worldLabel.setFont(FontManager.getRunescapeFont());
		titleWrapper.add(worldLabel);
		overallInfo.add(titleWrapper);

		// Add last activity label
		activityLabel.setBorder(new EmptyBorder(0, 0, 2, 0));
		activityLabel.setHorizontalAlignment(SwingConstants.LEFT);
		activityLabel.setFont(FontManager.getRunescapeSmallFont());
		overallInfo.add(activityLabel);

		// Add gimp status data
		JPanel statusWrapper = new JPanel();
		statusWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		statusWrapper.setLayout(new DynamicGridLayout(2, 1, 0, 2));
		statusWrapper.setBorder(new EmptyBorder(2, 0, 2, 0));
		// HP icon and bar
		JPanel hpWrapper = new JPanel();
		hpWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		hpWrapper.setLayout(new DynamicGridLayout(1, 2, 2, 0));
		hpBar.setBackground(HP_BG);
		hpBar.setForeground(HP_FG);
		String hpUri = "/skill_icons_small/hitpoints.png";
		ImageIcon hpIcon = new ImageIcon(ImageUtil.loadImageResource(getClass(), hpUri));
		JLabel hpLabel = new JLabel(hpIcon);
		hpWrapper.add(hpLabel);
		hpWrapper.add(hpBar);
		// Prayer icon and bar
		JPanel prayerWrapper = new JPanel();
		prayerWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		prayerWrapper.setLayout(new DynamicGridLayout(1, 2, 2, 0));
		prayerBar.setBackground(PRAYER_BG);
		prayerBar.setForeground(PRAYER_FG);
		String prayerUri = "/skill_icons_small/prayer.png";
		ImageIcon prayerIcon = new ImageIcon(ImageUtil.loadImageResource(getClass(), prayerUri));
		JLabel prayerLabel = new JLabel(prayerIcon);
		prayerWrapper.add(prayerLabel);
		prayerWrapper.add(prayerBar);
		// Add HP and prayer data to info panel
		statusWrapper.add(hpWrapper);
		statusWrapper.add(prayerWrapper);
		overallInfo.add(statusWrapper);

		// Add overall info to the container
		overallPanel.add(overallInfo);
		return overallPanel;
	}

	/**
	 * Builds a JPanel displaying an icon and level/number associated with it.
	 */
	private JPanel makeHiscorePanel(HiscoreSkill skill)
	{
		assert SwingUtilities.isEventDispatchThread();

		HiscoreSkillType skillType = skill == null ? HiscoreSkillType.SKILL : skill.getType();

		JLabel label = new JLabel();
		label.setToolTipText(skill == null ? "Combat" : skill.getName());
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setText(pad("--", skillType));

		String directory;
		if (skill == null || skill == OVERALL)
		{
			directory = "/skill_icons/";
		}
		else if (skill.getType() == HiscoreSkillType.BOSS)
		{
			directory = "bosses/";
		}
		else
		{
			directory = "/skill_icons_small/";
		}

		String skillName = (skill == null ? "combat" : skill.name().toLowerCase());
		String skillIcon = directory + skillName + ".png";
		log.debug("Loading skill icon from {}", skillIcon);

		label.setIcon(new ImageIcon(ImageUtil.loadImageResource(getClass(), skillIcon)));

		boolean totalLabel = skill == OVERALL || skill == null; // overall or combat
		label.setIconTextGap(totalLabel ? 10 : 4);

		JPanel skillPanel = new JPanel();
		skillPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		skillPanel.setBorder(new EmptyBorder(2, 0, 2, 0));
		skillLabels.put(skill, label);
		skillPanel.add(label);

		return skillPanel;
	}

	private void loadGimpData()
	{
		// If for some reason no tab was selected, default to normal
		if (selectedGimp == null)
		{
			resetSelectedTab();
		}

		// Sanity check, GIM clan channel definitely loaded by now
		ClanChannel gimClanChannel = client.getClanChannel(ClanID.GROUP_IRONMAN);
		if (gimClanChannel == null)
		{
			loading = false;
			return;
		}

		loading = true;

		// Display gimp data
		GimPlayer gimp = group.getGimp(selectedGimp);
		SwingUtilities.invokeLater(() ->
		{
			applyGimpData(gimp);

			// Apply hiscore date separately
			for (Map.Entry<HiscoreSkill, JLabel> entry : skillLabels.entrySet())
			{
				HiscoreSkill skill = entry.getKey();
				JLabel label = entry.getValue();
				HiscoreSkillType skillType = skill == null ? HiscoreSkillType.SKILL : skill.getType();

				label.setText(pad("--", skillType));
				label.setToolTipText(skill == null ? "Combat" : skill.getName());
			}
		});

		group.getHiscores(selectedGimp).whenCompleteAsync((result, ex) ->
			SwingUtilities.invokeLater(() ->
			{
				if (!selectedGimp.equals(selectedGimp))
				{
					// Selected gimp has changed in the meantime
					return;
				}

				if (result == null)
				{
					loading = false;
					return;
				}

				// Successful player lookup
				loading = false;
				fillGimpStatusData(gimp, result);
				applyHiscoreResult(result);
			}));
	}

	public void updateGimpData(GimPlayer gimpData)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (selectedGimp != null && selectedGimp.equals(gimpData.getName()))
			{
				GimPlayer gimp = group.getGimp(selectedGimp);
				if (gimpData.getHp() != null || gimpData.getMaxHp() != null)
				{
					int hpValue = gimpData.getHp() != null ? gimpData.getHp() : gimp.getHp();
					int maxHpValue = gimpData.getMaxHp() != null ? gimpData.getMaxHp() : gimp.getMaxHp();
					setHpBar(selectedGimp, hpValue, maxHpValue);
				}
				if (gimpData.getPrayer() != null || gimpData.getMaxPrayer() != null)
				{
					int prayerValue = gimpData.getPrayer() != null ? gimpData.getPrayer() : gimp.getPrayer();
					int maxPrayerValue = gimpData.getMaxPrayer() != null ? gimpData.getMaxPrayer() : gimp.getMaxPrayer();
					setPrayerBar(selectedGimp, prayerValue, maxPrayerValue);
				}
				if (gimpData.getLastActivity() != null)
				{
					setLastActivity(selectedGimp, gimpData.getLastActivity(), gimp.getWorld());
				}
				// Update more gimp data...
			}
		});
	}

	public void setConnectionStatus(boolean status)
	{
		String statusText = status ? "Connected" : "Disconnected";
		String openingTags = "<html><body style = 'padding: 5px;color:#989898'>";
		String closingTags = "</body></html>";
		String tooltipText = "<p><span style = 'color:white'>" + "You are currently "
			+ statusText.toLowerCase(Locale.ROOT) + "." + "</span></p>";
		String helpText = "<p><span style = 'color:white'>" + "To connect, please follow "
			+ "the instructions on the plugin help page." + "</span></p>";
		connectionLabel.setText(statusText);
		connectionLabel.setForeground(status ? Color.CYAN : ColorScheme.LIGHT_GRAY_COLOR);
		connectionLabel.setToolTipText(openingTags + tooltipText + (status ? "" : " " + helpText) + closingTags);
	}

	public void setHpBar(String gimpName, Integer hp, Integer maxHp)
	{
		if (selectedGimp != null && selectedGimp.equals(gimpName))
		{
			hpBar.setValue(formatStatusValue(hp));
			hpBar.setMaximumValue(formatStatusValue(maxHp));
			hpBar.setCenterLabel(formatStatusValue(hp) + "/" + formatStatusValue(maxHp));
		}
	}

	public void setPrayerBar(String gimpName, Integer prayer, Integer maxPrayer)
	{
		if (selectedGimp != null && selectedGimp.equals(gimpName))
		{
			prayerBar.setValue(formatStatusValue(prayer));
			prayerBar.setMaximumValue(formatStatusValue(maxPrayer));
			prayerBar.setCenterLabel(formatStatusValue(prayer) + "/" + formatStatusValue(maxPrayer));
		}
	}

	public void setLastActivity(String gimpName, String activity, int world)
	{
		if (selectedGimp != null && selectedGimp.equals(gimpName))
		{
			String activityText;

			// If activity is empty or reserved IN_GAME_ACTIVITY, use generic text
			if (activity == null || activity.isEmpty() || activity.equals(GimPlayer.IN_GAME_ACTIVITY))
			{
				activityText = world == 0 ? htmlLabelStr("Last activity:", "inactive") : htmlLabelStr("Currently:", "in game");
			}
			// Select descriptor text based on whether gimp is logged in
			else if (world == 0)
			{
				activityText = htmlLabelStr("Last activity:", activity.toLowerCase(Locale.ROOT));
			}
			else
			{
				activityText = htmlLabelStr("Currently:", "training " + activity.toLowerCase(Locale.ROOT));
			}
			activityLabel.setText(activityText);
		}
	}

	public void setWorld(String gimpName, int world)
	{
		if (selectedGimp != null && selectedGimp.equals(gimpName))
		{
			worldLabel.setText(world != 0 ? "W" + world : "Offline");
			worldLabel.setForeground(world != 0 ? Color.GREEN : Color.RED);
		}
	}

	private void applyGimpData(GimPlayer gimp)
	{
		assert SwingUtilities.isEventDispatchThread();

		String gimpName = gimp.getName();
		usernameLabel.setText(gimpName);

		setWorld(gimpName, group.getCurrentWorld(gimpName));
		setHpBar(gimpName, gimp.getHp(), gimp.getMaxHp());
		setPrayerBar(gimpName, gimp.getPrayer(), gimp.getMaxPrayer());
		setLastActivity(gimpName, gimp.getLastActivity(), gimp.getWorld());
	}

	/**
	 * Uses the hiscores result to fill missing HP/prayer status data.
	 *
	 * @param result HiscoreResult
	 */
	private void fillGimpStatusData(GimPlayer gimp, HiscoreResult result)
	{
		assert SwingUtilities.isEventDispatchThread();

		if (gimp.getMaxHp() == null)
		{
			setHpBar(gimp.getName(), gimp.getHp(), result.getHitpoints().getLevel());
		}
		if (gimp.getMaxPrayer() == null)
		{
			setPrayerBar(gimp.getName(), gimp.getPrayer(), result.getPrayer().getLevel());
		}
	}

	private void applyHiscoreResult(HiscoreResult result)
	{
		assert SwingUtilities.isEventDispatchThread();

		for (Map.Entry<HiscoreSkill, JLabel> entry : skillLabels.entrySet())
		{
			HiscoreSkill skill = entry.getKey();
			JLabel label = entry.getValue();
			Skill s;

			if (skill == null)
			{
				if (result.getPlayer() != null)
				{
					int combatLevel = Experience.getCombatLevel(
						result.getAttack().getLevel(),
						result.getStrength().getLevel(),
						result.getDefence().getLevel(),
						result.getHitpoints().getLevel(),
						result.getMagic().getLevel(),
						result.getRanged().getLevel(),
						result.getPrayer().getLevel()
					);
					label.setText(Integer.toString(combatLevel));
				}
			}
			else if ((s = result.getSkill(skill)) != null)
			{
				final long exp = s.getExperience();
				final boolean isSkill = skill.getType() == HiscoreSkillType.SKILL;
				int level = -1;
				if (!isSkill || exp != -1L)
				{
					// For skills, level is only valid if exp is not -1
					// otherwise level is always valid
					level = s.getLevel();
				}

				if (level != -1)
				{
					label.setText(pad(formatLevel(level), skill.getType()));
				}
			}

			label.setToolTipText(detailsHtml(result, skill));
		}
	}

	/**
	 * Builds a html string to display on tooltip (when hovering a skill).
	 */
	private String detailsHtml(HiscoreResult result, HiscoreSkill skill)
	{
		String openingTags = "<html><body style = 'padding: 5px;color:#989898'>";
		String closingTags = "</body></html>";

		String content = "";

		if (skill == null)
		{
			double combatLevel = Experience.getCombatLevelPrecise(
				result.getAttack().getLevel(),
				result.getStrength().getLevel(),
				result.getDefence().getLevel(),
				result.getHitpoints().getLevel(),
				result.getMagic().getLevel(),
				result.getRanged().getLevel(),
				result.getPrayer().getLevel()
			);

			double combatExperience = result.getAttack().getExperience()
				+ result.getStrength().getExperience() + result.getDefence().getExperience()
				+ result.getHitpoints().getExperience() + result.getMagic().getExperience()
				+ result.getRanged().getExperience() + result.getPrayer().getExperience();

			content += "<p><span style = 'color:white'>Combat</span></p>";
			content += "<p><span style = 'color:white'>Exact Combat Level:</span> " + QuantityFormatter.formatNumber(combatLevel) + "</p>";
			content += "<p><span style = 'color:white'>Experience:</span> " + QuantityFormatter.formatNumber(combatExperience) + "</p>";
		}
		else
		{
			switch (skill)
			{
				case CLUE_SCROLL_ALL:
				{
					String allRank = (result.getClueScrollAll().getRank() == -1) ? "Unranked" : QuantityFormatter.formatNumber(result.getClueScrollAll().getRank());
					String beginnerRank = (result.getClueScrollBeginner().getRank() == -1) ? "Unranked" : QuantityFormatter.formatNumber(result.getClueScrollBeginner().getRank());
					String easyRank = (result.getClueScrollEasy().getRank() == -1) ? "Unranked" : QuantityFormatter.formatNumber(result.getClueScrollEasy().getRank());
					String mediumRank = (result.getClueScrollMedium().getRank() == -1) ? "Unranked" : QuantityFormatter.formatNumber(result.getClueScrollMedium().getRank());
					String hardRank = (result.getClueScrollHard().getRank() == -1) ? "Unranked" : QuantityFormatter.formatNumber(result.getClueScrollHard().getRank());
					String eliteRank = (result.getClueScrollElite().getRank() == -1) ? "Unranked" : QuantityFormatter.formatNumber(result.getClueScrollElite().getRank());
					String masterRank = (result.getClueScrollMaster().getRank() == -1) ? "Unranked" : QuantityFormatter.formatNumber(result.getClueScrollMaster().getRank());
					String all = (result.getClueScrollAll().getLevel() == -1 ? "0" : QuantityFormatter.formatNumber(result.getClueScrollAll().getLevel()));
					String beginner = (result.getClueScrollBeginner().getLevel() == -1 ? "0" : QuantityFormatter.formatNumber(result.getClueScrollBeginner().getLevel()));
					String easy = (result.getClueScrollEasy().getLevel() == -1 ? "0" : QuantityFormatter.formatNumber(result.getClueScrollEasy().getLevel()));
					String medium = (result.getClueScrollMedium().getLevel() == -1 ? "0" : QuantityFormatter.formatNumber(result.getClueScrollMedium().getLevel()));
					String hard = (result.getClueScrollHard().getLevel() == -1 ? "0" : QuantityFormatter.formatNumber(result.getClueScrollHard().getLevel()));
					String elite = (result.getClueScrollElite().getLevel() == -1 ? "0" : QuantityFormatter.formatNumber(result.getClueScrollElite().getLevel()));
					String master = (result.getClueScrollMaster().getLevel() == -1 ? "0" : QuantityFormatter.formatNumber(result.getClueScrollMaster().getLevel()));
					content += "<p><span style = 'color:white'>Clues</span></p>";
					content += "<p><span style = 'color:white'>All:</span> " + all + " <span style = 'color:white'>Rank:</span> " + allRank + "</p>";
					content += "<p><span style = 'color:white'>Beginner:</span> " + beginner + " <span style = 'color:white'>Rank:</span> " + beginnerRank + "</p>";
					content += "<p><span style = 'color:white'>Easy:</span> " + easy + " <span style = 'color:white'>Rank:</span> " + easyRank + "</p>";
					content += "<p><span style = 'color:white'>Medium:</span> " + medium + " <span style = 'color:white'>Rank:</span> " + mediumRank + "</p>";
					content += "<p><span style = 'color:white'>Hard:</span> " + hard + " <span style = 'color:white'>Rank:</span> " + hardRank + "</p>";
					content += "<p><span style = 'color:white'>Elite:</span> " + elite + " <span style = 'color:white'>Rank:</span> " + eliteRank + "</p>";
					content += "<p><span style = 'color:white'>Master:</span> " + master + " <span style = 'color:white'>Rank:</span> " + masterRank + "</p>";
					break;
				}
				case BOUNTY_HUNTER_ROGUE:
				{
					Skill bountyHunterRogue = result.getBountyHunterRogue();
					String rank = (bountyHunterRogue.getRank() == -1) ? "Unranked" : QuantityFormatter.formatNumber(bountyHunterRogue.getRank());
					content += "<p><span style = 'color:white'>Bounty Hunter - Rogue</span></p>";
					content += "<p><span style = 'color:white'>Rank:</span> " + rank + "</p>";
					if (bountyHunterRogue.getLevel() > -1)
					{
						content += "<p><span style = 'color:white'>Score:</span> " + QuantityFormatter.formatNumber(bountyHunterRogue.getLevel()) + "</p>";
					}
					break;
				}
				case BOUNTY_HUNTER_HUNTER:
				{
					Skill bountyHunterHunter = result.getBountyHunterHunter();
					String rank = (bountyHunterHunter.getRank() == -1) ? "Unranked" : QuantityFormatter.formatNumber(bountyHunterHunter.getRank());
					content += "<p><span style = 'color:white'>Bounty Hunter - Hunter</span></p>";
					content += "<p><span style = 'color:white'>Rank:</span> " + rank + "</p>";
					if (bountyHunterHunter.getLevel() > -1)
					{
						content += "<p><span style = 'color:white'>Score:</span> " + QuantityFormatter.formatNumber(bountyHunterHunter.getLevel()) + "</p>";
					}
					break;
				}
				case LAST_MAN_STANDING:
				{
					Skill lastManStanding = result.getLastManStanding();
					String rank = (lastManStanding.getRank() == -1) ? "Unranked" : QuantityFormatter.formatNumber(lastManStanding.getRank());
					content += "<p><span style = 'color:white'>Last Man Standing</span></p>";
					content += "<p><span style = 'color:white'>Rank:</span> " + rank + "</p>";
					if (lastManStanding.getLevel() > -1)
					{
						content += "<p><span style = 'color:white'>Score:</span> " + QuantityFormatter.formatNumber(lastManStanding.getLevel()) + "</p>";
					}
					break;
				}
				case SOUL_WARS_ZEAL:
				{
					Skill soulWarsZeal = result.getSoulWarsZeal();
					String rank = (soulWarsZeal.getRank() == -1) ? "Unranked" : QuantityFormatter.formatNumber(soulWarsZeal.getRank());
					content += "<p><span style = 'color:white'>Soul Wars Zeal</span></p>";
					content += "<p><span style = 'color:white'>Rank:</span> " + rank + "</p>";
					if (soulWarsZeal.getLevel() > -1)
					{
						content += "<p><span style = 'color:white'>Score:</span> " + QuantityFormatter.formatNumber(soulWarsZeal.getLevel()) + "</p>";
					}
					break;
				}
				case LEAGUE_POINTS:
				{
					Skill leaguePoints = result.getLeaguePoints();
					String rank = (leaguePoints.getRank() == -1) ? "Unranked" : QuantityFormatter.formatNumber(leaguePoints.getRank());
					content += "<p><span style = 'color:white'>League Points</span></p>";
					content += "<p><span style = 'color:white'>Rank:</span> " + rank + "</p>";
					if (leaguePoints.getLevel() > -1)
					{
						content += "<p><span style = 'color:white'>Points:</span> " + QuantityFormatter.formatNumber(leaguePoints.getLevel()) + "</p>";
					}
					break;
				}
				case OVERALL:
				{
					Skill requestedSkill = result.getSkill(skill);
					String rank = (requestedSkill.getRank() == -1) ? "Unranked" : QuantityFormatter.formatNumber(requestedSkill.getRank());
					String exp = (requestedSkill.getExperience() == -1L) ? "Unranked" : QuantityFormatter.formatNumber(requestedSkill.getExperience());
					content += "<p><span style = 'color:white'>" + skill.getName() + "</span></p>";
					content += "<p><span style = 'color:white'>Rank:</span> " + rank + "</p>";
					content += "<p><span style = 'color:white'>Experience:</span> " + exp + "</p>";
					break;
				}
				default:
				{
					if (skill.getType() == HiscoreSkillType.BOSS)
					{
						String rank = "Unranked";
						String lvl = null;
						Skill requestedSkill = result.getSkill(skill);
						if (requestedSkill != null)
						{
							if (requestedSkill.getRank() > -1)
							{
								rank = QuantityFormatter.formatNumber(requestedSkill.getRank());
							}
							if (requestedSkill.getLevel() > -1)
							{
								lvl = QuantityFormatter.formatNumber(requestedSkill.getLevel());
							}
						}

						content += "<p><span style = 'color:white'>Boss:</span> " + skill.getName() + "</p>";
						content += "<p><span style = 'color:white'>Rank:</span> " + rank + "</p>";
						if (lvl != null)
						{
							content += "<p><span style = 'color:white'>KC:</span> " + lvl + "</p>";
						}
					}
					else
					{
						Skill requestedSkill = result.getSkill(skill);
						final long experience = requestedSkill.getExperience();

						String rank = (requestedSkill.getRank() == -1) ? "Unranked" : QuantityFormatter.formatNumber(requestedSkill.getRank());
						String exp = (experience == -1L) ? "Unranked" : QuantityFormatter.formatNumber(experience);
						String remainingXp;
						if (experience == -1L)
						{
							remainingXp = "Unranked";
						}
						else
						{
							int currentLevel = Experience.getLevelForXp((int) experience);
							remainingXp = (currentLevel + 1 <= Experience.MAX_VIRT_LEVEL) ? QuantityFormatter.formatNumber(Experience.getXpForLevel(currentLevel + 1) - experience) : "0";
						}

						content += "<p><span style = 'color:white'>Skill:</span> " + skill.getName() + "</p>";
						content += "<p><span style = 'color:white'>Rank:</span> " + rank + "</p>";
						content += "<p><span style = 'color:white'>Experience:</span> " + exp + "</p>";
						content += "<p><span style = 'color:white'>Remaining XP:</span> " + remainingXp + "</p>";
					}
					break;
				}
			}
		}

		// Add a html progress bar to the hover information
		if (skill != null && skill.getType() == HiscoreSkillType.SKILL)
		{
			long experience = result.getSkill(skill).getExperience();
			if (experience >= 0)
			{
				int currentXp = (int) experience;
				int currentLevel = Experience.getLevelForXp(currentXp);
				int xpForCurrentLevel = Experience.getXpForLevel(currentLevel);
				int xpForNextLevel = currentLevel + 1 <= Experience.MAX_VIRT_LEVEL ? Experience.getXpForLevel(currentLevel + 1) : -1;

				double xpGained = currentXp - xpForCurrentLevel;
				double xpGoal = xpForNextLevel != -1 ? xpForNextLevel - xpForCurrentLevel : 100;
				int progress = (int) ((xpGained / xpGoal) * 100f);

				// Had to wrap the bar with an empty div, adding the margin directly to the bar causes issues
				content += "<div style = 'margin-top:3px'>"
					+ "<div style = 'background: #070707; border: 1px solid #070707; height: 6px; width: 100%;'>"
					+ "<div style = 'height: 6px; width: " + progress + "%; background: #dc8a00;'>"
					+ "</div>"
					+ "</div>"
					+ "</div>";
			}
		}

		return openingTags + content + closingTags;
	}

	private static String htmlLabelInt(String key, int value)
	{
		final String valueStr = QuantityFormatter.quantityToStackSize(value);
		return String.format(HTML_LABEL_TEMPLATE, ColorUtil.toHexColor(ColorScheme.LIGHT_GRAY_COLOR), key, valueStr);
	}

	private static String htmlLabelStr(String key, String value)
	{
		String SPACE_CHAR = " ";
		return "<html><body style = 'color:#a5a5a5'>" + key + SPACE_CHAR
			+ "<span style = 'color:white'>" + value + "</span></body></html>";
	}

	private void resetSelectedTab()
	{
		tabGroup.select(tabGroup.getTab(defaultTab));
	}

	private static int formatStatusValue(Integer statusValue)
	{
		if (statusValue == null)
		{
			return 0;
		}
		return statusValue;
	}

	private static String formatLevel(int level)
	{
		if (level < 10000)
		{
			return Integer.toString(level);
		}
		else
		{
			return (level / 1000) + "k";
		}
	}

	private static String pad(String str, HiscoreSkillType type)
	{
		// Left pad label text to keep labels aligned
		int pad = type == HiscoreSkillType.BOSS ? 4 : 2;
		return StringUtils.leftPad(str, pad);
	}
}
