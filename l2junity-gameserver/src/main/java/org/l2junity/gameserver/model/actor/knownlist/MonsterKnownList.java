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

import org.l2junity.gameserver.ai.CharacterAI;
import org.l2junity.gameserver.ai.CtrlEvent;
import org.l2junity.gameserver.ai.CtrlIntention;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.L2MonsterInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;

public class MonsterKnownList extends AttackableKnownList
{
	public MonsterKnownList(L2MonsterInstance activeChar)
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
		
		final CharacterAI ai = getActiveChar().getAI(); // force AI creation
		
		// Set the L2MonsterInstance Intention to AI_INTENTION_ACTIVE if the state was AI_INTENTION_IDLE
		if ((object instanceof PlayerInstance) && (ai != null) && (ai.getIntention() == CtrlIntention.AI_INTENTION_IDLE))
		{
			ai.setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
		}
		
		return true;
	}
	
	@Override
	protected boolean removeKnownObject(WorldObject object, boolean forget)
	{
		if (!super.removeKnownObject(object, forget))
		{
			return false;
		}
		
		if (!(object instanceof Creature))
		{
			return true;
		}
		
		if (getActiveChar().hasAI())
		{
			// Notify the L2MonsterInstance AI with EVT_FORGET_OBJECT
			getActiveChar().getAI().notifyEvent(CtrlEvent.EVT_FORGET_OBJECT, object);
		}
		
		if (getActiveChar().isVisible() && getKnownPlayers().isEmpty() && getKnownSummons().isEmpty())
		{
			// Clear the _aggroList of the L2MonsterInstance
			getActiveChar().clearAggroList();
		}
		
		return true;
	}
	
	@Override
	public final L2MonsterInstance getActiveChar()
	{
		return (L2MonsterInstance) super.getActiveChar();
	}
}
