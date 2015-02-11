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

import java.util.Map;
import java.util.Map.Entry;

import org.l2junity.gameserver.model.PremiumItem;
import org.l2junity.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Gnacik
 */
public class ExGetPremiumItemList extends L2GameServerPacket
{
	private final L2PcInstance _activeChar;
	
	private final Map<Integer, PremiumItem> _map;
	
	public ExGetPremiumItemList(L2PcInstance activeChar)
	{
		_activeChar = activeChar;
		_map = _activeChar.getPremiumItemList();
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x87);
		writeD(_map.size());
		for (Entry<Integer, PremiumItem> entry : _map.entrySet())
		{
			PremiumItem item = entry.getValue();
			writeQ(entry.getKey());
			writeD(item.getItemId());
			writeQ(item.getCount());
			writeD(0x00); // ?
			writeS(item.getSender());
		}
	}
}
