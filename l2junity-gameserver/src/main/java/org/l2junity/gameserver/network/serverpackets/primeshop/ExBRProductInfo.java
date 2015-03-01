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
package org.l2junity.gameserver.network.serverpackets.primeshop;

import org.l2junity.gameserver.model.primeshop.PrimeShopGroup;
import org.l2junity.gameserver.model.primeshop.PrimeShopItem;
import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.gameserver.network.serverpackets.IGameServerPacket;
import org.l2junity.network.PacketWriter;

/**
 * @author Gnacik
 */
public class ExBRProductInfo implements IGameServerPacket
{
	private final PrimeShopGroup _item;
	
	public ExBRProductInfo(PrimeShopGroup item)
	{
		_item = item;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_BR_PRODUCT_INFO.writeId(packet);
		
		packet.writeD(_item.getBrId());
		packet.writeD(_item.getPrice());
		packet.writeD(_item.getItems().size());
		for (PrimeShopItem item : _item.getItems())
		{
			packet.writeD(item.getId());
			packet.writeD((int) item.getCount());
			packet.writeD(item.getWeight());
			packet.writeD(item.isTradable());
		}
		return true;
	}
}
