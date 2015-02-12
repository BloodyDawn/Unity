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
import java.util.Collection;
import java.util.List;

import org.l2junity.gameserver.handler.ITargetTypeHandler;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.L2PcInstance;
import org.l2junity.gameserver.model.entity.TvTEvent;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.L2TargetType;

/**
 * @author UnAfraid
 */
public class PartyClan implements ITargetTypeHandler
{
	@Override
	public WorldObject[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		List<Creature> targetList = new ArrayList<>();
		if (onlyFirst)
		{
			return new Creature[]
			{
				activeChar
			};
		}
		
		final L2PcInstance player = activeChar.getActingPlayer();
		
		if (player == null)
		{
			return EMPTY_TARGET_LIST;
		}
		
		targetList.add(player);
		
		final int radius = skill.getAffectRange();
		final boolean hasClan = player.getClan() != null;
		final boolean hasParty = player.isInParty();
		
		if (Skill.addPet(activeChar, player, radius, false))
		{
			targetList.add(player.getPet());
		}
		
		player.getServitors().values().forEach(s ->
		{
			if (Skill.addCharacter(activeChar, s, radius, false))
			{
				targetList.add(s);
			}
		});
		
		// if player in clan and not in party
		if (!(hasClan || hasParty))
		{
			return targetList.toArray(new Creature[targetList.size()]);
		}
		
		// Get all visible objects in a spherical area near the L2Character
		final Collection<L2PcInstance> objs = activeChar.getKnownList().getKnownPlayersInRadius(radius);
		int maxTargets = skill.getAffectLimit();
		for (L2PcInstance obj : objs)
		{
			if (obj == null)
			{
				continue;
			}
			
			// olympiad mode - adding only own side
			if (player.isInOlympiadMode())
			{
				if (!obj.isInOlympiadMode())
				{
					continue;
				}
				if (player.getOlympiadGameId() != obj.getOlympiadGameId())
				{
					continue;
				}
				if (player.getOlympiadSide() != obj.getOlympiadSide())
				{
					continue;
				}
			}
			
			if (player.isInDuel())
			{
				if (player.getDuelId() != obj.getDuelId())
				{
					continue;
				}
				
				if (hasParty && obj.isInParty() && (player.getParty().getLeaderObjectId() != obj.getParty().getLeaderObjectId()))
				{
					continue;
				}
			}
			
			if (!((hasClan && (obj.getClanId() == player.getClanId())) || (hasParty && obj.isInParty() && (player.getParty().getLeaderObjectId() == obj.getParty().getLeaderObjectId()))))
			{
				continue;
			}
			
			// Don't add this target if this is a Pc->Pc pvp
			// casting and pvp condition not met
			if (!player.checkPvpSkill(obj, skill))
			{
				continue;
			}
			
			if (!TvTEvent.checkForTvTSkill(player, obj, skill))
			{
				continue;
			}
			
			if (Skill.addPet(activeChar, obj, radius, false))
			{
				targetList.add(obj.getPet());
			}
			
			obj.getServitors().values().forEach(s ->
			{
				if (Skill.addCharacter(activeChar, s, radius, false))
				{
					targetList.add(s);
				}
			});
			
			if (!Skill.addCharacter(activeChar, obj, radius, false))
			{
				continue;
			}
			
			if ((maxTargets > 0) && (targetList.size() >= maxTargets))
			{
				break;
			}
			
			targetList.add(obj);
		}
		return targetList.toArray(new Creature[targetList.size()]);
	}
	
	@Override
	public Enum<L2TargetType> getTargetType()
	{
		return L2TargetType.PARTY_CLAN;
	}
}
