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
import java.util.concurrent.TimeUnit;

import org.l2junity.gameserver.enums.MatchingMemberType;
import org.l2junity.gameserver.instancemanager.InstanceManager;
import org.l2junity.gameserver.instancemanager.MapRegionManager;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.matching.PartyMatchingRoom;

/**
 * @author Gnacik
 */
public class ExPartyRoomMember extends L2GameServerPacket
{
	private final PartyMatchingRoom _room;
	private final MatchingMemberType _type;
	
	public ExPartyRoomMember(PlayerInstance player, PartyMatchingRoom room)
	{
		_room = room;
		_type = room.getMemberType(player);
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x08);
		writeD(_type.ordinal());
		writeD(_room.getMembersCount());
		for (PlayerInstance member : _room.getMembers())
		{
			writeD(member.getObjectId());
			writeS(member.getName());
			writeD(member.getActiveClass());
			writeD(member.getLevel());
			writeD(MapRegionManager.getInstance().getBBs(member.getLocation()));
			writeD(_room.getMemberType(member).ordinal());
			final Map<Integer, Long> _instanceTimes = InstanceManager.getInstance().getAllInstanceTimes(member.getObjectId());
			writeD(_instanceTimes.size());
			for (Entry<Integer, Long> entry : _instanceTimes.entrySet())
			{
				final long instanceTime = TimeUnit.MILLISECONDS.toSeconds(entry.getValue() - System.currentTimeMillis());
				writeD(entry.getKey());
				writeD((int) instanceTime);
			}
		}
	}
}