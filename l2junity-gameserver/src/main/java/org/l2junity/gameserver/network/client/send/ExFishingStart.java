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

import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.network.client.OutgoingPackets;
import org.l2junity.network.PacketWriter;

/**
 * @author -Wooden-
 */
public class ExFishingStart implements IClientOutgoingPacket
{
	private final Creature _activeChar;
	private final int _x, _y, _z, _fishType;
	private final boolean _isNightLure;
	
	public ExFishingStart(Creature character, int fishType, int x, int y, int z, boolean isNightLure)
	{
		_activeChar = character;
		_fishType = fishType;
		_x = x;
		_y = y;
		_z = z;
		_isNightLure = isNightLure;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_FISHING_START.writeId(packet);
		
		packet.writeD(_activeChar.getObjectId());
		packet.writeC(_fishType); // fish type
		packet.writeD(_x); // x position
		packet.writeD(_y); // y position
		packet.writeD(_z); // z position
		packet.writeC(_isNightLure ? 0x01 : 0x00); // night lure
		return true;
	}
}
