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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.model.skills.AbnormalType;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.skills.Skill;

/**
 * Synergy effect implementation.
 */
public final class Synergy extends AbstractEffect
{
	private final Set<AbnormalType> _requiredSlots;
	private final Set<AbnormalType> _optionalSlots;
	private final int _partyBuffSkillId;
	private final int _skillLevelScaleTo;
	private final int _minSlot;
	
	public Synergy(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		String requiredSlots = params.getString("requiredSlots", null);
		if ((requiredSlots != null) && !requiredSlots.isEmpty())
		{
			_requiredSlots = new HashSet<>();
			for (String slot : requiredSlots.split(";"))
			{
				_requiredSlots.add(AbnormalType.getAbnormalType(slot));
			}
		}
		else
		{
			_requiredSlots = Collections.<AbnormalType> emptySet();
		}
		
		String optionalSlots = params.getString("optionalSlots", null);
		if ((optionalSlots != null) && !optionalSlots.isEmpty())
		{
			_optionalSlots = new HashSet<>();
			for (String slot : optionalSlots.split(";"))
			{
				_optionalSlots.add(AbnormalType.getAbnormalType(slot));
			}
		}
		else
		{
			_optionalSlots = Collections.<AbnormalType> emptySet();
		}
		
		_partyBuffSkillId = params.getInt("partyBuffSkillId");
		_skillLevelScaleTo = params.getInt("skillLevelScaleTo", 1);
		_minSlot = params.getInt("minSlot", 2);
	}
	
	@Override
	public boolean onActionTime(BuffInfo info)
	{
		if (info.getEffector().isDead())
		{
			return false;
		}
		
		for (AbnormalType required : _requiredSlots)
		{
			if (info.getEffector().getEffectList().getBuffInfoByAbnormalType(required) == null)
			{
				return info.getSkill().isToggle();
			}
		}
		
		final int abnormalCount = (int) _optionalSlots.stream().filter(a -> info.getEffector().getEffectList().getBuffInfoByAbnormalType(a) != null).count();
		
		if (abnormalCount >= _minSlot)
		{
			final SkillHolder partyBuff = new SkillHolder(_partyBuffSkillId, Math.max(abnormalCount - 1, _skillLevelScaleTo));
			final Skill partyBuffSkill = partyBuff.getSkill();
			
			if (partyBuffSkill != null)
			{
				partyBuffSkill.forEachTargetAffected(info.getEffector(), info.getEffected(), target ->
				{
					if (target.isCreature())
					{
						info.getEffector().callSkill(partyBuffSkill, null, (Creature) target);
					}
				});
			}
			else
			{
				_log.warn("Skill not found effect called from {}", info.getSkill());
			}
		}
		
		return info.getSkill().isToggle();
	}
}
