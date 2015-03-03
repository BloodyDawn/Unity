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

import java.util.logging.Logger;

import org.l2junity.gameserver.network.client.ExIncomingPackets;
import org.l2junity.gameserver.network.client.L2GameClient;
import org.l2junity.network.IIncomingPacket;
import org.l2junity.network.PacketReader;

/**
 * @author Nos
 */
public class ExPacket implements IIncomingPacket<L2GameClient>
{
	private static final Logger LOGGER = Logger.getLogger(ExPacket.class.getName());
	
	private ExIncomingPackets _exIncomingPacket;
	private IIncomingPacket<L2GameClient> _exPacket;
	
	@Override
	public boolean read(PacketReader packet)
	{
		int exPacketId = packet.readH() & 0xFFFF;
		if ((exPacketId < 0) || (exPacketId >= ExIncomingPackets.PACKET_ARRAY.length))
		{
			return false;
		}
		
		_exIncomingPacket = ExIncomingPackets.PACKET_ARRAY[exPacketId];
		if (_exIncomingPacket == null)
		{
			LOGGER.fine(getClass().getSimpleName() + ": Unknown packet: " + Integer.toHexString(exPacketId));
			return false;
		}
		
		_exPacket = _exIncomingPacket.newIncomingPacket();
		return (_exPacket != null) && _exPacket.read(packet);
	}
	
	@Override
	public void run(L2GameClient client) throws Exception
	{
		if (!_exIncomingPacket.getConnectionStates().contains(client.getConnectionState()))
		{
			LOGGER.warning(" Connection at invalid state: " + client.getConnectionState() + " Required State: " + _exIncomingPacket.getConnectionStates());
			return;
		}
		_exPacket.run(client);
	}
}
