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
package handlers.targethandlers.affectobject;

import org.l2junity.gameserver.handler.IAffectObjectHandler;
import org.l2junity.gameserver.model.L2Clan;
import org.l2junity.gameserver.model.Party;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.FriendlyNpcInstance;
import org.l2junity.gameserver.model.actor.instance.L2FriendlyMobInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.skills.targets.AffectObject;
import org.l2junity.gameserver.model.zone.ZoneId;

/**
 * @author Nik
 */
public class NotFriend implements IAffectObjectHandler
{
	@Override
	public boolean checkAffectedObject(Creature activeChar, Creature target)
	{
		if (activeChar == target)
		{
			return false;
		}
		
		final PlayerInstance player = activeChar.getActingPlayer();
		final PlayerInstance targetPlayer = target.getActingPlayer();
		
		if (player != null)
		{
			if (targetPlayer != null)
			{
				if (player == targetPlayer)
				{
					return false;
				}
				
				// Players in peace zone are not considered enemies.
				if (target.isInsideZone(ZoneId.PEACE))
				{
					return false;
				}
				
				final Party party = player.getParty();
				final Party targetParty = targetPlayer.getParty();
				if (party != null)
				{
					if (party == targetParty)
					{
						return false;
					}
					
					if ((targetParty != null) && (party.getCommandChannel() == targetParty.getCommandChannel()))
					{
						return false;
					}
				}
				
				if (target.isInsideZone(ZoneId.PVP))
				{
					return true;
				}
				
				// Duel
				if (player.isInDuel() && targetPlayer.isInDuel())
				{
					if (player.getDuelId() == targetPlayer.getDuelId())
					{
						return true;
					}
				}
				
				// Olympiad
				if (player.isInOlympiadMode() && targetPlayer.isInOlympiadMode())
				{
					if (player.getOlympiadGameId() == targetPlayer.getOlympiadGameId())
					{
						return true;
					}
				}
				
				final L2Clan clan = player.getClan();
				final L2Clan targetClan = targetPlayer.getClan();
				if (clan != null)
				{
					if (clan == targetClan)
					{
						return false;
					}
					
					// War
					if ((targetClan != null) && clan.isAtWarWith(targetClan) && targetClan.isAtWarWith(clan))
					{
						return true;
					}
				}
				
				if ((player.getAllyId() != 0) && (player.getAllyId() == targetPlayer.getAllyId()))
				{
					return false;
				}
				
				if (target.isInsideZone(ZoneId.SIEGE) && ((player.getSiegeSide() == 0) || ((player.getSiegeState() > 0) && (player.getSiegeState() != targetPlayer.getSiegeState()))))
				{
					return true;
				}
				
				// By default any flagged/PK player is considered enemy.
				return (target.getActingPlayer().getPvpFlag() > 0) || (target.getActingPlayer().getReputation() < 0);
			}
			
			return target.isMonster();
		}
		else if (activeChar.isNpc())
		{
			Npc npc = (Npc) activeChar;
			
			if (target.isNpc())
			{
				// TODO: Check enemy clans.
				
				// By default any other npc is a friend.
				return false;
			}
			
			// Friendly NPCs are friends with players.
			if ((targetPlayer != null) && ((npc instanceof FriendlyNpcInstance) || (npc instanceof L2FriendlyMobInstance)))
			{
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public Enum<AffectObject> getAffectObjectType()
	{
		return AffectObject.NOT_FRIEND;
	}
}
