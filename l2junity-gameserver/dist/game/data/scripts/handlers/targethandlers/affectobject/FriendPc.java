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
public class FriendPc implements IAffectObjectHandler
{
	@Override
	public boolean checkAffectedObject(Creature activeChar, Creature target)
	{
		if (activeChar == target)
		{
			return true;
		}
		
		final PlayerInstance player = activeChar.getActingPlayer();
		final PlayerInstance targetPlayer = target.getActingPlayer();
		
		if (player != null)
		{
			if (targetPlayer != null)
			{
				if (player == targetPlayer)
				{
					return true;
				}
				
				final Party party = player.getParty();
				final Party targetParty = targetPlayer.getParty();
				if (party != null)
				{
					if (party == targetParty)
					{
						return true;
					}
					
					if ((targetParty != null) && (party.getCommandChannel() == targetParty.getCommandChannel()))
					{
						return true;
					}
				}
				
				if (target.isInsideZone(ZoneId.PVP))
				{
					return false;
				}
				
				// Duel
				if (player.isInDuel() && targetPlayer.isInDuel())
				{
					if (player.getDuelId() == targetPlayer.getDuelId())
					{
						return false;
					}
				}
				
				// Olympiad
				if (player.isInOlympiadMode() && targetPlayer.isInOlympiadMode())
				{
					if (player.getOlympiadGameId() == targetPlayer.getOlympiadGameId())
					{
						return false;
					}
				}
				
				final L2Clan clan = player.getClan();
				final L2Clan targetClan = targetPlayer.getClan();
				if (clan != null)
				{
					if (clan == targetClan)
					{
						return true;
					}
					
					if ((targetClan != null) && (clan.isAtWarWith(targetClan) || targetClan.isAtWarWith(clan)))
					{
						return false;
					}
				}
				
				if ((player.getAllyId() != 0) && (player.getAllyId() == targetPlayer.getAllyId()))
				{
					return true;
				}
				
				if ((player.getSiegeState() > 0) && player.isInsideZone(ZoneId.SIEGE) && (player.getSiegeState() == targetPlayer.getSiegeState()) && (player.getSiegeSide() == targetPlayer.getSiegeSide()))
				{
					return true;
				}
				
				// By default any neutral non-flagged player is considered a friend.
				return (target.getActingPlayer().getPvpFlag() == 0) || (target.getActingPlayer().getReputation() >= 0);
			}
		}
		else if (activeChar.isNpc())
		{
			Npc npc = (Npc) activeChar;
			
			// Friendly NPCs are friends with players.
			if ((targetPlayer != null) && ((npc instanceof FriendlyNpcInstance) || (npc instanceof L2FriendlyMobInstance)))
			{
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public Enum<AffectObject> getAffectObjectType()
	{
		return AffectObject.FRIEND_PC;
	}
}
