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

import org.l2junity.Config;
import org.l2junity.gameserver.handler.ITargetTypeHandler;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.L2TargetType;
import org.l2junity.gameserver.util.Util;

/**
 * @author UnAfraid
 */
public class PartyNotMe implements ITargetTypeHandler
{
	@Override
	public WorldObject[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		final List<Creature> targetList = new ArrayList<>();
		final int radius = skill.getAffectRange();
		if (activeChar.getParty() != null)
		{
			final List<PlayerInstance> partyList = activeChar.getParty().getMembers();
			for (PlayerInstance partyMember : partyList)
			{
				if (partyMember == activeChar)
				{
					continue;
				}
				else if (!Util.checkIfInRange(Config.ALT_PARTY_RANGE, activeChar, partyMember, true))
				{
					continue;
				}
				else
				{
					
					if (Skill.addPet(activeChar, partyMember, radius, false))
					{
						targetList.add(partyMember.getPet());
					}
					
					partyMember.getServitors().values().forEach(s ->
					{
						if (Skill.addCharacter(activeChar, s, radius, false))
						{
							targetList.add(s);
						}
					});
					
					if (Skill.addCharacter(activeChar, partyMember, radius, false))
					{
						targetList.add(partyMember);
					}
					
					targetList.add(partyMember);
				}
			}
		}
		return targetList.toArray(new Creature[targetList.size()]);
	}
	
	@Override
	public Enum<L2TargetType> getTargetType()
	{
		return L2TargetType.PARTY_NOTME;
	}
}
