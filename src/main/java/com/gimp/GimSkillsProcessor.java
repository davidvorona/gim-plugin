package com.gimp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;

public class GimSkillsProcessor
{
	private static final List<Skill> SKILLS = Arrays.asList(Skill.values());

	private void assertValidSkill(Skill skill)
	{
		if (!SKILLS.contains(skill))
		{
			throw new IllegalArgumentException("Invalid skill: " + skill);
		}
	}

	public Map<Skill, Integer> initSkillsXp(int[] xp)
	{
		Map<Skill, Integer> skillsXp = new HashMap<>();
		for (Skill skill : SKILLS)
		{
			skillsXp.put(skill, xp[skill.ordinal()]);
		}
		return skillsXp;
	}

	public void applySkillsXp(Map<Skill, Integer> skillsXp, Map<Skill, Integer> xpUpdate)
	{
		for (Skill skill : xpUpdate.keySet())
		{
			assertValidSkill(skill);
			skillsXp.put(skill, xpUpdate.get(skill));
		}
	}

	public Map<Skill, Integer> processXpDiff(Map<Skill, Integer> skillsXp, Map<Skill, Integer> xpUpdate)
	{
		Map<Skill, Integer> xpDiff = new HashMap<>();
		for (Skill skill : xpUpdate.keySet())
		{
			assertValidSkill(skill);
			int diff = xpUpdate.get(skill) - skillsXp.get(skill);
			xpDiff.put(skill, diff);
		}
		return xpDiff;
	}

	public Map<Skill, Integer> createXpUpdate(Skill skill, int xp)
	{
		assertValidSkill(skill);
		Map<Skill, Integer> xpUpdate = new HashMap<>();
		xpUpdate.put(skill, xp);
		return xpUpdate;
	}
}
