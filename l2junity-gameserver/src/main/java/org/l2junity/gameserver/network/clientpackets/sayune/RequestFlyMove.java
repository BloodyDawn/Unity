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
package org.l2junity.gameserver.network.clientpackets.sayune;

import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.actor.request.SayuneRequest;
import org.l2junity.gameserver.network.clientpackets.L2GameClientPacket;

/**
 * @author UnAfraid
 */
public class RequestFlyMove extends L2GameClientPacket
{
	int _locationId;
	
	@Override
	protected void readImpl()
	{
		_locationId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final PlayerInstance activeChar = getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		final SayuneRequest request = activeChar.getRequest(SayuneRequest.class);
		if (request == null)
		{
			return;
		}
		
		request.move(activeChar, _locationId);
	}
}
