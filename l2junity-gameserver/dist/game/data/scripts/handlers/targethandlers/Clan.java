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
import org.l2junity.gameserver.model.ClanMember;
import org.l2junity.gameserver.model.L2Clan;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.L2TargetType;

/**
 * @author UnAfraid
 */
public class Clan implements ITargetTypeHandler
{
	@Override
	public Creature[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		List<Creature> targetList = new ArrayList<>();
		
		if (activeChar.isPlayable())
		{
			final PlayerInstance player = activeChar.getActingPlayer();
			
			if (player == null)
			{
				return EMPTY_TARGET_LIST;
			}
			
			if (player.isInOlympiadMode())
			{
				return new Creature[]
				{
					player
				};
			}
			
			if (onlyFirst)
			{
				return new Creature[]
				{
					player
				};
			}
			
			targetList.add(player);
			
			final int radius = skill.getAffectRange();
			final L2Clan clan = player.getClan();
			
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
			
			if (clan != null)
			{
				PlayerInstance obj;
				for (ClanMember member : clan.getMembers())
				{
					obj = member.getPlayerInstance();
					
					if ((obj == null) || (obj == player))
					{
						continue;
					}
					
					if (player.isInDuel())
					{
						if (player.getDuelId() != obj.getDuelId())
						{
							continue;
						}
						if (player.isInParty() && obj.isInParty() && (player.getParty().getLeaderObjectId() != obj.getParty().getLeaderObjectId()))
						{
							continue;
						}
					}
					
					// Don't add this target if this is a Pc->Pc pvp casting and pvp condition not met
					if (!player.checkPvpSkill(obj, skill))
					{
						continue;
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
						continue;
					}
					
					if (onlyFirst)
					{
						return new Creature[]
						{
							obj
						};
					}
					
					targetList.add(obj);
				}
			}
		}
		else if (activeChar.isNpc())
		{
			// for buff purposes, returns friendly mobs nearby and mob itself
			final Npc npc = (Npc) activeChar;
			if ((npc.getTemplate().getClans() == null) || npc.getTemplate().getClans().isEmpty())
			{
				return new Creature[]
				{
					activeChar
				};
			}
			
			targetList.add(activeChar);
			
			for (Npc newTarget : World.getInstance().getVisibleObjects(activeChar, Npc.class, skill.getCastRange()))
			{
				if (npc.isInMyClan(newTarget))
				{
					final int maxTargets = skill.getAffectLimit();
					if ((maxTargets > 0) && (targetList.size() >= maxTargets))
					{
						break;
					}
					
					if ((skill.getAffectHeightMin() != 0) && (skill.getAffectHeightMax() != 0))
					{
						if (((activeChar.getZ() + skill.getAffectHeightMin()) > newTarget.getZ()) || ((activeChar.getZ() + skill.getAffectHeightMax()) < newTarget.getZ()))
						{
							continue;
						}
					}
					
					targetList.add(newTarget);
				}
			}
		}
		
		return targetList.toArray(new Creature[targetList.size()]);
	}
	
	@Override
	public Enum<L2TargetType> getTargetType()
	{
		return L2TargetType.CLAN;
	}
}
