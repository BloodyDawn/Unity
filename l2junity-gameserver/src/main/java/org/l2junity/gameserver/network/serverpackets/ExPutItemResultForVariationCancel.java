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

import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.network.PacketWriter;

public class ExPutItemResultForVariationCancel implements IGameServerPacket
{
	private final int _itemObjId;
	private final int _itemId;
	private final int _itemAug1;
	private final int _itemAug2;
	private final int _price;
	
	public ExPutItemResultForVariationCancel(ItemInstance item, int price)
	{
		_itemObjId = item.getObjectId();
		_itemId = item.getDisplayId();
		_price = price;
		_itemAug1 = ((short) item.getAugmentation().getAugmentationId());
		_itemAug2 = item.getAugmentation().getAugmentationId() >> 16;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_PUT_ITEM_RESULT_FOR_VARIATION_CANCEL.writeId(packet);
		
		packet.writeD(_itemObjId);
		packet.writeD(_itemId);
		packet.writeD(_itemAug1);
		packet.writeD(_itemAug2);
		packet.writeQ(_price);
		packet.writeD(0x01);
		return true;
	}
}
