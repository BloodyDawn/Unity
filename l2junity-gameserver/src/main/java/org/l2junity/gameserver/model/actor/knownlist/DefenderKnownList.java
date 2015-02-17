/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver.model.actor.knownlist;

import org.l2junity.gameserver.ai.CtrlIntention;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.instance.L2DefenderInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.entity.Castle;
import org.l2junity.gameserver.model.entity.Fort;
import org.l2junity.gameserver.model.entity.clanhall.SiegableHall;

public class DefenderKnownList extends AttackableKnownList
{
	public DefenderKnownList(L2DefenderInstance activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public boolean addKnownObject(WorldObject object)
	{
		if (!super.addKnownObject(object))
		{
			return false;
		}
		
		Castle castle = getActiveChar().getCastle();
		Fort fortress = getActiveChar().getFort();
		SiegableHall hall = getActiveChar().getConquerableHall();
		// Check if siege is in progress
		if (((fortress != null) && fortress.getZone().isActive()) || ((castle != null) && castle.getZone().isActive()) || ((hall != null) && hall.getSiegeZone().isActive()))
		{
			PlayerInstance player = null;
			if (object.isPlayable())
			{
				player = object.getActingPlayer();
			}
			int activeSiegeId = (fortress != null ? fortress.getResidenceId() : (castle != null ? castle.getResidenceId() : hall != null ? hall.getId() : 0));
			
			// Check if player is an enemy of this defender npc
			if ((player != null) && (((player.getSiegeState() == 2) && !player.isRegisteredOnThisSiegeField(activeSiegeId)) || ((player.getSiegeState() == 1)) || (player.getSiegeState() == 0)))
			{
				if (getActiveChar().getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
				{
					getActiveChar().getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
				}
			}
		}
		return true;
	}
	
	@Override
	public final L2DefenderInstance getActiveChar()
	{
		return (L2DefenderInstance) super.getActiveChar();
	}
}
