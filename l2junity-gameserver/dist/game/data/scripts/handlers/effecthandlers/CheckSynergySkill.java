/*
 * Copyright (C) 2004-2015 L2J Unity
 * 
 * This file is part of L2J Unity.
 * 
 * L2J Unity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Unity is distributed in the hope that it will be useful,
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
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.skills.AbnormalType;
import org.l2junity.gameserver.model.skills.BuffInfo;

/**
 * @author Sdw
 */
public class CheckSynergySkill extends AbstractEffect
{
	private final Set<AbnormalType> _requiredSlots;
	private final Set<AbnormalType> _optionalSlots;
	private final int _minSlot;
	
	public CheckSynergySkill(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
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
		_minSlot = params.getInt("minSlot", 2);
	}
	
	@Override
	public boolean onActionTime(BuffInfo info)
	{
		if (info.getEffected().isPlayer())
		{
			final PlayerInstance player = info.getEffected().getActingPlayer();
			
			if (_requiredSlots.stream().allMatch(a -> info.getEffector().getEffectList().getBuffInfoByAbnormalType(a) != null))
			{
				final int abnormalCount = (int) _optionalSlots.stream().filter(a -> info.getEffector().getEffectList().getBuffInfoByAbnormalType(a) != null).count();
				
				if (abnormalCount >= _minSlot)
				{
					return info.getSkill().isToggle();
				}
			}
			
			player.stopSkillEffects(info.getSkill());
		}
		
		return info.getSkill().isToggle();
	}
}
