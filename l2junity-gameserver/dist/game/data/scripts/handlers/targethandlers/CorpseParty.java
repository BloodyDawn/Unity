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
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.entity.TvTEvent;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.L2TargetType;
import org.l2junity.gameserver.model.zone.ZoneId;

/**
 * @author UnAfraid
 */
public class CorpseParty implements ITargetTypeHandler
{
	@Override
	public WorldObject[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		List<WorldObject> targetList = new ArrayList<>();
		if (activeChar.isPlayable())
		{
			final PlayerInstance player = activeChar.getActingPlayer();
			if (player == null)
			{
				return EMPTY_TARGET_LIST;
			}
			
			if (player.isInOlympiadMode())
			{
				return new WorldObject[]
				{
					player
				};
			}
			
			final Party party = player.getParty();
			if (party != null)
			{
				final int radius = skill.getAffectRange();
				final int maxTargets = skill.getAffectLimit();
				for (PlayerInstance member : party.getMembers())
				{
					if ((member == null) || (member == player))
					{
						continue;
					}
					
					if (player.isInDuel())
					{
						if (player.getDuelId() != member.getDuelId())
						{
							continue;
						}
						if (player.isInParty() && member.isInParty() && (player.getParty().getLeaderObjectId() != member.getParty().getLeaderObjectId()))
						{
							continue;
						}
					}
					
					// Don't add this target if this is a Pc->Pc pvp casting and pvp condition not met
					if (!player.checkPvpSkill(member, skill))
					{
						continue;
					}
					
					if (!TvTEvent.checkForTvTSkill(player, member, skill))
					{
						continue;
					}
					
					if (!Skill.addCharacter(activeChar, member, radius, true))
					{
						continue;
					}
					
					// check target is not in a active siege zone
					if (member.isInsideZone(ZoneId.SIEGE) && !member.isInSiege())
					{
						continue;
					}
					
					if (onlyFirst)
					{
						return new WorldObject[]
						{
							member
						};
					}
					
					if ((maxTargets > 0) && (targetList.size() >= maxTargets))
					{
						break;
					}
					
					targetList.add(member);
				}
			}
		}
		else if (activeChar.isNpc()) // I don't know what is an NPC party, so take NPC clan
		{
			// for buff purposes, returns friendly mobs nearby and mob itself
			final Npc npc = (Npc) activeChar;
			if ((npc.getTemplate().getClans() == null) || npc.getTemplate().getClans().isEmpty())
			{
				return new WorldObject[]
				{
					activeChar
				};
			}
			
			targetList.add(activeChar);
			
			for (Npc newTarget : World.getInstance().getVisibleObjects(activeChar, Npc.class, skill.getCastRange()))
			{
				if (npc.isInMyClan(newTarget))
				{
					if (targetList.size() >= skill.getAffectLimit())
					{
						break;
					}
					
					targetList.add(newTarget);
				}
			}
		}
		
		return targetList.toArray(new WorldObject[targetList.size()]);
	}
	
	@Override
	public Enum<L2TargetType> getTargetType()
	{
		return L2TargetType.CORPSE_PARTY;
	}
}
