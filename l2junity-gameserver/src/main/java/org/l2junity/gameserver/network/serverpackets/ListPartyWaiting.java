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

import java.util.LinkedList;
import java.util.List;

import org.l2junity.gameserver.instancemanager.MatchingRoomManager;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.matching.MatchingRoom;

/**
 * @author Gnacik
 */
public class ListPartyWaiting extends L2GameServerPacket
{
	private final List<MatchingRoom> _rooms = new LinkedList<>();
	private final int _size;
	
	private static final int NUM_PER_PAGE = 64;
	
	public ListPartyWaiting(int level, int location, int page)
	{
		final List<MatchingRoom> rooms = MatchingRoomManager.getInstance().getPartyMathchingRooms(location, level);
		
		_size = rooms.size();
		final int startIndex = (page - 1) * NUM_PER_PAGE;
		int chunkSize = _size - startIndex;
		if (chunkSize > NUM_PER_PAGE)
		{
			chunkSize = NUM_PER_PAGE;
		}
		for (int i = startIndex; i < (startIndex + chunkSize); i++)
		{
			_rooms.add(rooms.get(i));
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x9C);
		writeD(_size);
		writeD(_rooms.size());
		for (MatchingRoom room : _rooms)
		{
			writeD(room.getId());
			writeS(room.getTitle());
			writeD(room.getLocation());
			writeD(room.getMinLvl());
			writeD(room.getMaxLvl());
			writeD(room.getMaxMembers());
			writeS(room.getLeader().getName());
			writeD(room.getMembersCount());
			for (PlayerInstance member : room.getMembers())
			{
				writeD(member.getClassId().getId());
				writeS(member.getName());
			}
		}
	}
}
