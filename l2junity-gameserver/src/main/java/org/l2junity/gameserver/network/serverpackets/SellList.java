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

import java.util.List;

import javolution.util.FastList;

import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.actor.instance.L2MerchantInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.network.PacketWriter;

/**
 * This class ...
 * @version $Revision: 1.4.2.3.2.4 $ $Date: 2005/03/27 15:29:39 $
 */
public class SellList implements IGameServerPacket
{
	private final PlayerInstance _activeChar;
	private final L2MerchantInstance _lease;
	private final long _money;
	private final List<ItemInstance> _selllist = new FastList<>();
	
	public SellList(PlayerInstance player)
	{
		_activeChar = player;
		_lease = null;
		_money = _activeChar.getAdena();
		doLease();
	}
	
	public SellList(PlayerInstance player, L2MerchantInstance lease)
	{
		_activeChar = player;
		_lease = lease;
		_money = _activeChar.getAdena();
		doLease();
	}
	
	private void doLease()
	{
		if (_lease == null)
		{
			final Summon pet = _activeChar.getPet();
			for (ItemInstance item : _activeChar.getInventory().getItems())
			{
				if (!item.isEquipped() && item.isSellable() && ((pet == null) || (item.getObjectId() != pet.getControlObjectId()))) // Pet is summoned and not the item that summoned the pet
				{
					_selllist.add(item);
				}
			}
		}
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.SELL_LIST.writeId(packet);
		
		packet.writeQ(_money);
		packet.writeD(_lease == null ? 0x00 : 1000000 + _lease.getTemplate().getId());
		packet.writeH(_selllist.size());
		
		for (ItemInstance item : _selllist)
		{
			packet.writeH(item.getItem().getType1());
			packet.writeD(item.getObjectId());
			packet.writeD(item.getDisplayId());
			packet.writeQ(item.getCount());
			packet.writeH(item.getItem().getType2());
			packet.writeH(item.isEquipped() ? 0x01 : 0x00);
			packet.writeD(item.getItem().getBodyPart());
			packet.writeH(item.getEnchantLevel());
			packet.writeH(0x00); // TODO: Verify me
			packet.writeH(item.getCustomType2());
			packet.writeQ(item.getItem().getReferencePrice() / 2);
			// T1
			packet.writeH(item.getAttackElementType());
			packet.writeH(item.getAttackElementPower());
			for (byte i = 0; i < 6; i++)
			{
				packet.writeH(item.getElementDefAttr(i));
			}
			// Enchant Effects
			for (int op : item.getEnchantOptions())
			{
				packet.writeH(op);
			}
		}
		return true;
	}
}
