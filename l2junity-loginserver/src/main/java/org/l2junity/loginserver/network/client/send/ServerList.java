/*
 * Copyright (C) 2004-2014 L2J Server
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
package org.l2junity.loginserver.network.client.send;

import org.l2junity.network.IOutgoingPacket;
import org.l2junity.network.PacketWriter;

/**
 * @author Nos
 */
public class ServerList implements IOutgoingPacket
{
	// private final ClientHandler _client;
	
	public ServerList()
	{
		// _client = client;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		// TODO: Implement me
		packet.writeC(0x04);
		
		packet.writeC(60); // Server Count
		packet.writeC(2); // Last Server ID
		
		for (int i = 1; i <= 60; i++)
		{
			// [cddcchhcdc]
			packet.writeC(i); // Server ID
			
			packet.writeC(127); // IP
			packet.writeC(0); // IP
			packet.writeC(0); // IP
			packet.writeC(1); // IP
			packet.writeD(7777); // Port
			
			packet.writeC(0); // AgeLimit.NONE(0), FIFTEEN(15), EIGHTEEN(18)
			packet.writeC(0); // PK Enabled
			packet.writeH(1); // Player Count
			packet.writeH(1); // Player Limit
			packet.writeC(0); // ServerState.OFFLINE(0), ONLINE(1)
			packet.writeD(0); // ServerType.RELAX(0x02), TEST(0x04), BROAD(0x08), CREATE_RESTRICT(0x10), EVENT(0x20), FREE(0x40), WORLD_RAID(0x100), NEW(0x200), CLASSIC(0x400)
			packet.writeC(0); // Puts [NULL] in front of name due to missing file in NA client i think its region
		}
		
		packet.writeH(0); // Unused by client
		
		packet.writeC(5);
		for (int i = 1; i <= 5; i++)
		{
			packet.writeC(i); // Server ID
			packet.writeC(127); // Character Count
			packet.writeC(50); // Deleted Character Count
			for (int j = 1; j <= 50; j++)
			{
				packet.writeD(j);
			}
		}
		
		return true;
	}
}
