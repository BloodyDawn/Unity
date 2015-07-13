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

import java.util.Map;

import org.l2junity.gameserver.data.sql.impl.ClanTable;
import org.l2junity.gameserver.instancemanager.ClanHallManager;
import org.l2junity.gameserver.model.entity.clanhall.AuctionableHall;
import org.l2junity.gameserver.network.client.OutgoingPackets;
import org.l2junity.network.PacketWriter;

/**
 * @author KenM
 */
public class ExShowAgitInfo implements IClientOutgoingPacket
{
	public static final ExShowAgitInfo STATIC_PACKET = new ExShowAgitInfo();
	
	private ExShowAgitInfo()
	{
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_SHOW_AGIT_INFO.writeId(packet);
		
		final Map<Integer, AuctionableHall> clannhalls = ClanHallManager.getInstance().getAllAuctionableClanHalls();
		packet.writeD(clannhalls.size());
		for (AuctionableHall ch : clannhalls.values())
		{
			packet.writeD(ch.getId());
			packet.writeS(ch.getOwnerId() <= 0 ? "" : ClanTable.getInstance().getClan(ch.getOwnerId()).getName()); // owner clan name
			packet.writeS(ch.getOwnerId() <= 0 ? "" : ClanTable.getInstance().getClan(ch.getOwnerId()).getLeaderName()); // leader name
			packet.writeD(ch.getGrade() > 0 ? 0x00 : 0x01); // 0 - auction 1 - war clanhall 2 - ETC (rainbow spring clanhall)
		}
		return true;
	}
}
