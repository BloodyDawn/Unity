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

import java.util.Collection;

import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.primeshop.PrimeShopGroup;
import org.l2junity.gameserver.model.primeshop.PrimeShopItem;
import org.l2junity.gameserver.network.serverpackets.L2GameServerPacket;

/**
 * @author UnAfraid
 */
public class ExBRProductList extends L2GameServerPacket
{
	private final PlayerInstance _activeChar;
	private final int _type;
	private final Collection<PrimeShopGroup> _primeList;
	
	public ExBRProductList(PlayerInstance activeChar, int type, Collection<PrimeShopGroup> items)
	{
		_activeChar = activeChar;
		_type = type;
		_primeList = items;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xFE);
		writeH(0xD7);
		writeQ(_activeChar.getAdena()); // Adena
		writeQ(0x00); // Hero coins
		writeC(_type); // Type 0 - Home, 1 - History, 2 - Favorites
		writeD(_primeList.size());
		for (PrimeShopGroup brItem : _primeList)
		{
			writeD(brItem.getBrId());
			writeC(brItem.getCat());
			writeC(brItem.getPaymentType()); // Payment Type: 0 - Prime Points, 1 - Adena, 2 - Hero Coins
			writeD(brItem.getPrice());
			writeC(brItem.getPanelType()); // Item Panel Type: 0 - None, 1 - Event, 2 - Sale, 3 - New, 4 - Best
			writeD(brItem.getRecommended()); // Recommended: (bit flags) 1 - Top, 2 - Left, 4 - Right
			writeD(brItem.getStartSale());
			writeD(brItem.getEndSale());
			writeC(brItem.getDaysOfWeek());
			writeC(brItem.getStartHour());
			writeC(brItem.getStartMinute());
			writeC(brItem.getStopHour());
			writeC(brItem.getStopMinute());
			writeD(brItem.getStock());
			writeD(brItem.getTotal());
			writeC(brItem.getSalePercent());
			writeC(brItem.getMinLevel());
			writeC(brItem.getMaxLevel());
			writeD(brItem.getMinBirthday());
			writeD(brItem.getMaxBirthday());
			writeD(brItem.getRestrictionDay());
			writeD(brItem.getAvailableCount());
			writeC(brItem.getItems().size());
			for (PrimeShopItem item : brItem.getItems())
			{
				writeD(item.getId());
				writeD((int) item.getCount());
				writeD(item.getWeight());
				writeD(item.isTradable());
			}
		}
	}
}