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

import org.l2junity.gameserver.enums.MatchingMemberType;
import org.l2junity.gameserver.instancemanager.MapRegionManager;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.matching.CommandChannelMatchingRoom;

/**
 * @author Sdw
 */
public class ExMPCCRoomMember extends L2GameServerPacket
{
	private final CommandChannelMatchingRoom _room;
	private final MatchingMemberType _type;
	
	public ExMPCCRoomMember(PlayerInstance player, CommandChannelMatchingRoom room)
	{
		_room = room;
		_type = room.getMemberType(player);
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0xA0);
		
		writeD(_type.ordinal());
		writeD(_room.getMembersCount());
		for (PlayerInstance member : _room.getMembers())
		{
			writeD(member.getObjectId());
			writeS(member.getName());
			writeD(member.getLevel());
			writeD(member.getClassId().getId());
			writeD(MapRegionManager.getInstance().getBBs(member.getLocation()));
			writeD(_room.getMemberType(member).ordinal());
		}
	}
}
