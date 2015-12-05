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
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.skills.Skill;

/**
 * An Effect that does nothing except showing visual representation of a skill in the buff bar.
 * @author Nik
 */
public class VisualBuff extends AbstractEffect
{
	private final List<SkillHolder> _visualSkills = new ArrayList<>();
	
	public VisualBuff(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		for (Object param : params.getSet().values())
		{
			final int skillId = Integer.parseInt((String) param);
			_visualSkills.add(new SkillHolder(skillId, 1));
		}
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		effected.getEffectList().addVisualAbnormalStatus(_visualSkills);
	}
	
	@Override
	public void onExit(BuffInfo info)
	{
		info.getEffected().getEffectList().removeVisualAbnormalStatus(_visualSkills);
	}
}
