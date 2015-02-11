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

import org.l2junity.gameserver.MonsterRace;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.instance.L2RaceManagerInstance;
import org.l2junity.gameserver.network.serverpackets.DeleteObject;

public class RaceManagerKnownList extends NpcKnownList
{
	public RaceManagerKnownList(L2RaceManagerInstance activeChar)
	{
		super(activeChar);
	}
	
	@Override
	protected boolean removeKnownObject(WorldObject object, boolean forget)
	{
		if (!super.removeKnownObject(object, forget))
		{
			return false;
		}
		
		if (object.isPlayer())
		{
			for (int i = 0; i < 8; i++)
			{
				object.sendPacket(new DeleteObject(MonsterRace.getInstance().getMonsters()[i]));
			}
		}
		
		return true;
	}
	
	@Override
	public L2RaceManagerInstance getActiveChar()
	{
		return (L2RaceManagerInstance) super.getActiveChar();
	}
}
