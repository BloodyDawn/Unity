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
package org.l2junity.gameserver.network.client.send;

import java.util.List;

import org.l2junity.gameserver.data.sql.impl.ClanTable;
import org.l2junity.gameserver.model.L2Clan;
import org.l2junity.gameserver.network.client.OutgoingPackets;
import org.l2junity.network.PacketWriter;

/**
 * @author -Wooden-
 */
public class PledgeReceiveWarList implements IClientOutgoingPacket
{
	private final L2Clan _clan;
	private final int _tab;
	private final List<Integer> _clanList;
	
	public PledgeReceiveWarList(L2Clan clan, int tab)
	{
		_clan = clan;
		_tab = tab;
		_clanList = _tab == 0 ? _clan.getWarList() : _clan.getAttackerList();
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.PLEDGE_RECEIVE_WAR_LIST.writeId(packet);
		
		packet.writeD(_tab); // page
		packet.writeD(_clanList.size());
		for (Integer clanId : _clanList)
		{
			final L2Clan clan = ClanTable.getInstance().getClan(clanId);
			
			if (clan == null)
			{
				continue;
			}
			
			packet.writeS(clan.getName());
			packet.writeD(0x00); // type: 0 = Declaration, 1 = Blood Declaration, 2 = In War, 3 = Victory, 4 = Defeat, 5 = Tie, 6 = Error
			packet.writeD(0x00); // Time if friends to start remaining
			packet.writeD(0x00); // Score
			packet.writeD(0x00); // Recent change in points
			packet.writeD(0x00); // Friends to start war left
		}
		return true;
	}
}
