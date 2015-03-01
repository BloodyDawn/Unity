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
package org.l2junity.gameserver.network.serverpackets;

import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.network.PacketWriter;

/**
 * @author mrTJO
 */
public class Ex2ndPasswordAck implements IGameServerPacket
{
	private final int _status;
	private final int _response;
	
	// TODO: Enum
	public static int SUCCESS = 0x00;
	public static int WRONG_PATTERN = 0x01;
	
	public Ex2ndPasswordAck(int status, int response)
	{
		_status = status;
		_response = response;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_2ND_PASSWORD_ACK.writeId(packet);
		
		packet.writeC(_status);
		packet.writeD(_response == WRONG_PATTERN ? 0x01 : 0x00);
		packet.writeD(0x00);
		return true;
	}
}
