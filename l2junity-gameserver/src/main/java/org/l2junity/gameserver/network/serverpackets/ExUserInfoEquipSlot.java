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

import org.l2junity.gameserver.enums.InventorySlot;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.itemcontainer.PcInventory;

/**
 * @author Sdw
 */
public class ExUserInfoEquipSlot extends AbstractMaskPacket<InventorySlot>
{
	private final PlayerInstance _activeChar;
	
	private final byte[] _masks = new byte[]
	{
		(byte) 0x00,
		(byte) 0x00,
		(byte) 0x00,
		(byte) 0x00,
		(byte) 0x00
	};
	
	public ExUserInfoEquipSlot(PlayerInstance cha)
	{
		this(cha, true);
	}
	
	public ExUserInfoEquipSlot(PlayerInstance cha, boolean addAll)
	{
		_activeChar = cha;
		
		if (addAll)
		{
			addComponentType(InventorySlot.values());
		}
	}
	
	@Override
	protected byte[] getMasks()
	{
		return _masks;
	}
	
	@Override
	protected void onNewMaskAdded(InventorySlot component)
	{
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x156);
		
		writeD(_activeChar.getObjectId());
		writeH(InventorySlot.values().length);
		writeB(_masks);
		
		final PcInventory inventory = _activeChar.getInventory();
		for (InventorySlot slot : InventorySlot.values())
		{
			if (containsMask(slot))
			{
				writeH(18); // 2 + 4 * 4
				writeD(inventory.getPaperdollObjectId(slot.getSlot()));
				writeD(inventory.getPaperdollItemId(slot.getSlot()));
				writeD(inventory.getPaperdollAugmentationId(slot.getSlot()));
				writeD(inventory.getPaperdollItemVisualId(slot.getSlot()));
			}
		}
	}
}