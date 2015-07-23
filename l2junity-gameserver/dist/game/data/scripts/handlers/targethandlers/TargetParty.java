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
package handlers.targethandlers;

import java.util.ArrayList;
import java.util.List;

import org.l2junity.gameserver.handler.ITargetTypeHandler;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.L2TargetType;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * @author St3eT
 */
public class TargetParty implements ITargetTypeHandler
{
	@Override
	public Creature[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		List<Creature> targetList = new ArrayList<>();
		
		// Check for null target or any other invalid target
		if ((target == null) || target.isDead() || (target == activeChar))
		{
			activeChar.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
			return EMPTY_TARGET_LIST;
		}
		
		final int radius = skill.getAffectRange();
		final PlayerInstance player = (PlayerInstance) activeChar.getTarget();
		
		if (player.isInParty())
		{
			for (PlayerInstance partyMember : player.getParty().getMembers())
			{
				if ((partyMember == null))
				{
					continue;
				}
				
				if (addCharacter(player, partyMember, radius, false))
				{
					targetList.add(partyMember);
				}
				
				if (addPet(player, partyMember, radius, false))
				{
					targetList.add(partyMember.getPet());
				}
				
				partyMember.getServitors().values().forEach(s ->
				{
					if (addCharacter(activeChar, s, radius, false))
					{
						targetList.add(s);
					}
				});
			}
		}
		else
		{
			targetList.add(target);
		}
		return targetList.toArray(new Creature[targetList.size()]);
	}
	
	@Override
	public Enum<L2TargetType> getTargetType()
	{
		return L2TargetType.TARGET_PARTY;
	}
}
