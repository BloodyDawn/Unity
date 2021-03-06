/*
 * Copyright (C) 2004-2015 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.effecthandlers;

import java.util.ArrayList;
import java.util.List;

import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.skills.Skill;

/**
 * Resist Skill effect implementaion.
 * @author UnAfraid
 */
public final class ResistSkill extends AbstractEffect
{
	private final List<SkillHolder> _skills = new ArrayList<>();
	
	public ResistSkill(StatsSet params)
	{
		for (int i = 1;; i++)
		{
			int skillId = params.getInt("skillId" + i, 0);
			int skillLvl = params.getInt("skillLvl" + i, 0);
			if (skillId == 0)
			{
				break;
			}
			_skills.add(new SkillHolder(skillId, skillLvl));
		}
		
		if (_skills.isEmpty())
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + ": Without parameters!");
		}
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		for (SkillHolder holder : _skills)
		{
			effected.addIgnoreSkillEffects(holder);
			effected.sendDebugMessage("Applying invul against " + holder.getSkill());
		}
	}
	
	@Override
	public void onExit(BuffInfo info)
	{
		final Creature effected = info.getEffected();
		for (SkillHolder holder : _skills)
		{
			info.getEffected().removeIgnoreSkillEffects(holder);
			effected.sendDebugMessage("Removing invul against " + holder.getSkill());
		}
	}
}
