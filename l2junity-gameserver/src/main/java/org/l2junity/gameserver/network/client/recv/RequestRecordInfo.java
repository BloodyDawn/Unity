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
package org.l2junity.gameserver.network.client.recv;

import java.util.Collection;

import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.client.L2GameClient;
import org.l2junity.gameserver.network.client.send.SpawnItem;
import org.l2junity.gameserver.network.client.send.UserInfo;
import org.l2junity.network.PacketReader;

public class RequestRecordInfo implements IClientIncomingPacket
{
	@Override
	public boolean read(PacketReader packet)
	{
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		final PlayerInstance activeChar = client.getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		client.sendPacket(new UserInfo(activeChar));
		
		Collection<WorldObject> objs = activeChar.getKnownList().getKnownObjects().values();
		for (WorldObject object : objs)
		{
			if (object.getPoly().isMorphed() && object.getPoly().getPolyType().equals("item"))
			{
				client.sendPacket(new SpawnItem(object));
			}
			else
			{
				if (!object.isVisibleFor(activeChar))
				{
					object.sendInfo(activeChar);
					
					if (object instanceof Creature)
					{
						// Update the state of the L2Character object client
						// side by sending Server->Client packet
						// MoveToPawn/CharMoveToLocation and AutoAttackStart to
						// the L2PcInstance
						final Creature obj = (Creature) object;
						if (obj.getAI() != null)
						{
							obj.getAI().describeStateToPlayer(activeChar);
						}
					}
				}
			}
		}
	}
}
