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

import java.util.Comparator;
import java.util.List;

import org.l2junity.gameserver.instancemanager.CastleManager;
import org.l2junity.gameserver.model.entity.Castle;
import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.network.PacketWriter;

/**
 * @author l3x
 */
public final class ExSendManorList implements IGameServerPacket
{
	public static final ExSendManorList STATIC_PACKET = new ExSendManorList();
	
	private ExSendManorList()
	{
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		final List<Castle> castles = CastleManager.getInstance().getCastles();
		castles.sort(Comparator.comparing(Castle::getResidenceId));
		
		OutgoingPackets.EX_SEND_MANOR_LIST.writeId(packet);
		
		packet.writeD(castles.size());
		for (Castle castle : castles)
		{
			packet.writeD(castle.getResidenceId());
		}
		return true;
	}
}
