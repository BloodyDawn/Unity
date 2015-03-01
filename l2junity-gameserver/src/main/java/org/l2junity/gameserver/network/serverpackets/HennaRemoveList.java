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

import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.items.Henna;
import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.network.PacketWriter;

/**
 * @author Zoey76
 */
public class HennaRemoveList implements IGameServerPacket
{
	private final PlayerInstance _player;
	
	public HennaRemoveList(PlayerInstance player)
	{
		_player = player;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.HENNA_UNEQUIP_LIST.writeId(packet);
		
		packet.writeQ(_player.getAdena());
		packet.writeD(0x03); // seems to be max size
		packet.writeD(3 - _player.getHennaEmptySlots());
		
		for (Henna henna : _player.getHennaList())
		{
			if (henna != null)
			{
				packet.writeD(henna.getDyeId());
				packet.writeD(henna.getDyeItemId());
				packet.writeQ(henna.getCancelCount());
				packet.writeQ(henna.getCancelFee());
				packet.writeD(0x00);
				packet.writeD(0x00);
			}
		}
		return true;
	}
}
