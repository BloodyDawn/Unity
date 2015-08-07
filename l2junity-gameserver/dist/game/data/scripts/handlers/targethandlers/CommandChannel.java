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
import org.l2junity.gameserver.model.Party;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.L2TargetType;

/**
 * @author UnAfraid
 */
public class CommandChannel implements ITargetTypeHandler
{
	@Override
	public Creature[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		final List<Creature> targetList = new ArrayList<>();
		final PlayerInstance player = activeChar.getActingPlayer();
		if (player == null)
		{
			return EMPTY_TARGET_LIST;
		}
		
		targetList.add(player);
		
		final int radius = skill.getAffectRange();
		final Party party = player.getParty();
		final boolean hasChannel = (party != null) && party.isInCommandChannel();
		
		if (addPet(activeChar, player, radius, false))
		{
			targetList.add(player.getPet());
		}
		
		player.getServitors().values().forEach(s ->
		{
			if (addCharacter(activeChar, s, radius, false))
			{
				targetList.add(s);
			}
		});
		
		// if player in not in party
		if (party == null)
		{
			return targetList.toArray(new Creature[targetList.size()]);
		}
		
		// Get all visible objects in a spherical area near the L2Character
		int maxTargets = skill.getAffectLimit();
		final List<PlayerInstance> members = hasChannel ? party.getCommandChannel().getMembers() : party.getMembers();
		
		for (PlayerInstance member : members)
		{
			if (activeChar == member)
			{
				continue;
			}
			
			if ((skill.getAffectHeightMin() != 0) && (skill.getAffectHeightMax() != 0))
			{
				if (((activeChar.getZ() + skill.getAffectHeightMin()) > member.getZ()) || ((activeChar.getZ() + skill.getAffectHeightMax()) < member.getZ()))
				{
					continue;
				}
			}
			
			if (addCharacter(activeChar, member, radius, false))
			{
				targetList.add(member);
				if ((maxTargets > 0) && (targetList.size() >= maxTargets))
				{
					break;
				}
			}
		}
		
		return targetList.toArray(new Creature[targetList.size()]);
	}
	
	@Override
	public Enum<L2TargetType> getTargetType()
	{
		return L2TargetType.COMMAND_CHANNEL;
	}
}
