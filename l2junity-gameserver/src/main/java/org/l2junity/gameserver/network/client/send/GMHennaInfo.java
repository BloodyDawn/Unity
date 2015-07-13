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

import java.util.ArrayList;
import java.util.List;

import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.items.Henna;
import org.l2junity.gameserver.network.client.OutgoingPackets;
import org.l2junity.network.PacketWriter;

/**
 * This server packet sends the player's henna information using the Game Master's UI.
 * @author KenM, Zoey76
 */
public final class GMHennaInfo implements IClientOutgoingPacket
{
	private final PlayerInstance _activeChar;
	private final List<Henna> _hennas = new ArrayList<>();
	
	public GMHennaInfo(PlayerInstance player)
	{
		_activeChar = player;
		for (Henna henna : _activeChar.getHennaList())
		{
			if (henna != null)
			{
				_hennas.add(henna);
			}
		}
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.GMHENNA_INFO.writeId(packet);
		
		packet.writeC(_activeChar.getHennaStatINT()); // equip INT
		packet.writeC(_activeChar.getHennaStatSTR()); // equip STR
		packet.writeC(_activeChar.getHennaStatCON()); // equip CON
		packet.writeC(_activeChar.getHennaStatMEN()); // equip MEN
		packet.writeC(_activeChar.getHennaStatDEX()); // equip DEX
		packet.writeC(_activeChar.getHennaStatWIT()); // equip WIT
		packet.writeC(0x00); // equip LUC
		packet.writeC(0x00); // equip CHA
		packet.writeD(3); // Slots
		packet.writeD(_hennas.size()); // Size
		for (Henna henna : _hennas)
		{
			packet.writeD(henna.getDyeId());
			packet.writeD(0x01);
		}
		packet.writeD(0x00);
		packet.writeD(0x00);
		packet.writeD(0x00);
		return true;
	}
}
