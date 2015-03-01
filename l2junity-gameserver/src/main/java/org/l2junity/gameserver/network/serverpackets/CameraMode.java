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

public class CameraMode implements IGameServerPacket
{
	private final int _mode;
	
	/**
	 * Forces client camera mode change
	 * @param mode 0 - third person cam 1 - first person cam
	 */
	public CameraMode(int mode)
	{
		_mode = mode;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.CAMERA_MODE.writeId(packet);
		
		packet.writeD(_mode);
		return true;
	}
}
