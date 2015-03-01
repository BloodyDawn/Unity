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

import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.network.PacketWriter;

public class Earthquake implements IGameServerPacket
{
	private final int _x;
	private final int _y;
	private final int _z;
	private final int _intensity;
	private final int _duration;
	
	/**
	 * @param x
	 * @param y
	 * @param z
	 * @param intensity
	 * @param duration
	 */
	public Earthquake(int x, int y, int z, int intensity, int duration)
	{
		_x = x;
		_y = y;
		_z = z;
		_intensity = intensity;
		_duration = duration;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EARTHQUAKE.writeId(packet);
		
		packet.writeD(_x);
		packet.writeD(_y);
		packet.writeD(_z);
		packet.writeD(_intensity);
		packet.writeD(_duration);
		packet.writeD(0x00); // Unknown
		return true;
	}
}
