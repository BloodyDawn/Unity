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

import org.l2junity.gameserver.model.Party;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.util.Util;

/**
 * @author Sdw
 */
public class CheckForceSkill extends AbstractEffect
{
	private final int _skillId;
	
	public CheckForceSkill(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		_skillId = params.getInt("skillId");
	}
	
	@Override
	public boolean onActionTime(BuffInfo info)
	{
		if (info.getEffected().isPlayer())
		{
			final PlayerInstance player = info.getEffected().getActingPlayer();
			final Party party = player.getParty();
			if (party != null)
			{
				if (party.getMembers().stream().anyMatch(p -> p.isAffectedBySkill(_skillId) && Util.checkIfInRange(info.getSkill().getAffectRange(), p, player, false)))
				{
					return info.getSkill().isToggle();
				}
			}
			else if (player.isAffectedBySkill(_skillId))
			{
				return info.getSkill().isToggle();
			}
			
			player.stopSkillEffects(info.getSkill());
		}
		
		return info.getSkill().isToggle();
	}
}