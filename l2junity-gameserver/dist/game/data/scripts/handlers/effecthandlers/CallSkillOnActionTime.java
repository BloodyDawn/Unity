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

import org.l2junity.gameserver.handler.ITargetTypeHandler;
import org.l2junity.gameserver.handler.TargetHandler;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * Dam Over Time effect implementation.
 */
public final class CallSkillOnActionTime extends AbstractEffect
{
	private final SkillHolder _skill;
	private final double _power;
	
	public CallSkillOnActionTime(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_skill = new SkillHolder(params.getInt("skillId"), params.getInt("skillLevel", 1));
		_power = params.getDouble("power", 0);
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		castSkill(info);
	}
	
	@Override
	public boolean onActionTime(BuffInfo info)
	{
		return castSkill(info);
	}
	
	private boolean castSkill(BuffInfo info)
	{
		if (info.getEffector().isDead())
		{
			return false;
		}

		final Skill skill = _skill.getSkill();
		if (skill != null)
		{
			final ITargetTypeHandler targetHandler = TargetHandler.getInstance().getHandler(skill.getTargetType());
			
			final Creature[] targets = targetHandler.getTargetList(skill, info.getEffector(), false, info.getEffected());
			
			for (Creature target : targets)
			{
				if (skill.checkCondition(info.getEffector(), target))
				{
					info.getEffector().callSkill(skill, target);
				}
			}
		}
		else
		{
			_log.warn("Skill not found effect called from {}", info.getSkill());
		}
		return info.getSkill().isToggle();
	}
}
