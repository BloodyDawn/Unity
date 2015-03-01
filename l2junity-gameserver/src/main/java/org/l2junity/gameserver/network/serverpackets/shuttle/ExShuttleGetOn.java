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
package org.l2junity.gameserver.network.serverpackets.shuttle;

import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.actor.instance.L2ShuttleInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.gameserver.network.serverpackets.IGameServerPacket;
import org.l2junity.network.PacketWriter;

/**
 * @author UnAfraid
 */
public class ExShuttleGetOn implements IGameServerPacket
{
	private final int _playerObjectId, _shuttleObjectId;
	private final Location _pos;
	
	public ExShuttleGetOn(PlayerInstance player, L2ShuttleInstance shuttle)
	{
		_playerObjectId = player.getObjectId();
		_shuttleObjectId = shuttle.getObjectId();
		_pos = player.getInVehiclePosition();
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_SUTTLE_GET_ON.writeId(packet);
		
		packet.writeD(_playerObjectId);
		packet.writeD(_shuttleObjectId);
		packet.writeD(_pos.getX());
		packet.writeD(_pos.getY());
		packet.writeD(_pos.getZ());
		return true;
	}
}
