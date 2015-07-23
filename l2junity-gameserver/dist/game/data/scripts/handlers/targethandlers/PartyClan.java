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
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.L2TargetType;

/**
 * @author UnAfraid
 */
public class PartyClan implements ITargetTypeHandler
{
	@Override
	public Creature[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		List<Creature> targetList = new ArrayList<>();
		if (onlyFirst)
		{
			return new Creature[]
			{
				activeChar
			};
		}
		
		final PlayerInstance player = activeChar.getActingPlayer();
		
		if (player == null)
		{
			return EMPTY_TARGET_LIST;
		}
		
		targetList.add(player);
		
		final int radius = skill.getAffectRange();
		final boolean hasClan = player.getClan() != null;
		final boolean hasParty = player.isInParty();
		
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
		
		// if player in clan and not in party
		if (!(hasClan || hasParty))
		{
			return targetList.toArray(new Creature[targetList.size()]);
		}
		
		// Get all visible objects in a spherical area near the L2Character
		int maxTargets = skill.getAffectLimit();
		World.getInstance().forEachVisibleObjectInRange(activeChar, PlayerInstance.class, radius, obj ->
		{
			if (obj == null)
			{
				return;
			}
			
			// olympiad mode - adding only own side
			if (player.isInOlympiadMode())
			{
				if (!obj.isInOlympiadMode())
				{
					return;
				}
				if (player.getOlympiadGameId() != obj.getOlympiadGameId())
				{
					return;
				}
				if (player.getOlympiadSide() != obj.getOlympiadSide())
				{
					return;
				}
			}
			
			if (player.isInDuel())
			{
				if (player.getDuelId() != obj.getDuelId())
				{
					return;
				}
				
				if (hasParty && obj.isInParty() && (player.getParty().getLeaderObjectId() != obj.getParty().getLeaderObjectId()))
				{
					return;
				}
			}
			
			if (!((hasClan && (obj.getClanId() == player.getClanId())) || (hasParty && obj.isInParty() && (player.getParty().getLeaderObjectId() == obj.getParty().getLeaderObjectId()))))
			{
				return;
			}
			
			// Don't add this target if this is a Pc->Pc pvp
			// casting and pvp condition not met
			if (!player.checkPvpSkill(obj, skill))
			{
				return;
			}
			
			if (addPet(activeChar, obj, radius, false))
			{
				targetList.add(obj.getPet());
			}
			
			obj.getServitors().values().forEach(s ->
			{
				if (addCharacter(activeChar, s, radius, false))
				{
					targetList.add(s);
				}
			});
			
			if (!addCharacter(activeChar, obj, radius, false))
			{
				return;
			}
			
			if ((maxTargets > 0) && (targetList.size() >= maxTargets))
			{
				return;
			}
			
			targetList.add(obj);
		});
		return targetList.toArray(new Creature[targetList.size()]);
	}
	
	@Override
	public Enum<L2TargetType> getTargetType()
	{
		return L2TargetType.PARTY_CLAN;
	}
}
