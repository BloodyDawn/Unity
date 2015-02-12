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

import java.util.Collection;

import org.l2junity.gameserver.model.actor.instance.L2PcInstance;
import org.l2junity.gameserver.model.items.instance.ItemInstance;

/**
 * @author ShanSoft
 */
public class ExBuySellList extends AbstractItemPacket
{
	private Collection<ItemInstance> _sellList = null;
	private Collection<ItemInstance> _refundList = null;
	private final boolean _done;
	
	public ExBuySellList(L2PcInstance player, boolean done)
	{
		_sellList = player.getInventory().getAvailableItems(false, false, false);
		if (player.hasRefund())
		{
			_refundList = player.getRefund().getItems();
		}
		_done = done;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xFE);
		writeH(0xB8);
		writeD(0x01);
		writeD(0x00); // TODO: Find me
		
		if ((_sellList != null))
		{
			writeH(_sellList.size());
			for (ItemInstance item : _sellList)
			{
				writeItem(item);
				writeQ(item.getItem().getReferencePrice() / 2);
			}
		}
		else
		{
			writeH(0x00);
		}
		
		if ((_refundList != null) && !_refundList.isEmpty())
		{
			writeH(_refundList.size());
			int i = 0;
			for (ItemInstance item : _refundList)
			{
				writeItem(item);
				writeD(i++);
				writeQ((item.getItem().getReferencePrice() / 2) * item.getCount());
			}
		}
		else
		{
			writeH(0x00);
		}
		
		writeC(_done ? 0x01 : 0x00);
	}
}
