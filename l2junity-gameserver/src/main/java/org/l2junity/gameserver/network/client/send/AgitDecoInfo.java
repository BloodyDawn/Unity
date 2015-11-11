/*
 * Copyright (C) 2004-2015 L2J Unity
 * 
 * This file is part of L2J Unity.
 * 
 * L2J Unity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Unity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver.network.client.send;

import org.l2junity.gameserver.model.entity.ClanHall;
import org.l2junity.network.PacketWriter;

/**
 * @author Steuf
 */
public class AgitDecoInfo implements IClientOutgoingPacket
{
	private final ClanHall _clanHall;
	
	public AgitDecoInfo(ClanHall ClanHall)
	{
		_clanHall = ClanHall;
	}
	
	//@formatter:off
	/*
	  	Packet send, must be confirmed
	 	OutgoingPackets.CAMERA_MODE.writeInto(packet);
		packet.writeD(0); // clanhall id
		packet.writeC(0); // FUNC_RESTORE_HP (Fireplace)
		packet.writeC(0); // FUNC_RESTORE_MP (Carpet)
		packet.writeC(0); // FUNC_RESTORE_MP (Statue)
		packet.writeC(0); // FUNC_RESTORE_EXP (Chandelier)
		packet.writeC(0); // FUNC_TELEPORT (Mirror)
		packet.writeC(0); // Crytal
		packet.writeC(0); // Curtain
		packet.writeC(0); // FUNC_ITEM_CREATE (Magic Curtain)
		packet.writeC(0); // FUNC_SUPPORT
		packet.writeC(0); // FUNC_SUPPORT (Flag)
		packet.writeC(0); // Front Platform
		packet.writeC(0); // FUNC_ITEM_CREATE
		packet.writeD(0);
		packet.writeD(0);
	 */
	//@formatter:on
	@Override
	public boolean write(PacketWriter packet)
	{
		packet.writeC(0xFD);
		
		packet.writeD(_clanHall.getResidenceId());
		// FUNC_RESTORE_HP
		packet.writeC(0);
		// FUNC_RESTORE_MP
		packet.writeC(0);
		packet.writeC(0);
		// FUNC_RESTORE_EXP
		packet.writeC(0);
		// FUNC_TELEPORT
		packet.writeC(0);
		packet.writeC(0);
		// CURTAINS
		packet.writeC(0);
		// FUNC_ITEM_CREATE
		packet.writeC(0);
		// FUNC_SUPPORT
		packet.writeC(0);
		packet.writeC(0);
		// Front Plateform
		packet.writeC(0);
		// FUNC_ITEM_CREATE
		packet.writeC(0);
		// Unknown
		packet.writeD(0); // TODO: Find me!
		packet.writeD(0); // TODO: Find me!
		packet.writeD(0); // TODO: Find me!
		packet.writeD(0); // TODO: Find me!
		packet.writeD(0); // TODO: Find me!
		return true;
	}
}
