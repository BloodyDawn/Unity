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
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.l2junity.gameserver.instancemanager.InstanceManager;
import org.l2junity.gameserver.instancemanager.MatchingRoomManager;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.base.ClassId;
import org.l2junity.gameserver.model.instancezone.InstanceWorld;

/**
 * @author Gnacik
 */
public class ExListPartyMatchingWaitingRoom extends L2GameServerPacket
{
	private static final int NUM_PER_PAGE = 64;
	private final int _size;
	private final List<PlayerInstance> _players = new LinkedList<>();
	
	public ExListPartyMatchingWaitingRoom(PlayerInstance player, int page, int minLevel, int maxLevel, List<ClassId> classIds, String query)
	{
		final List<PlayerInstance> players = MatchingRoomManager.getInstance().getPlayerInWaitingList(minLevel, maxLevel, classIds, query);
		
		_size = players.size();
		final int startIndex = (page - 1) * NUM_PER_PAGE;
		int chunkSize = _size - startIndex;
		if (chunkSize > NUM_PER_PAGE)
		{
			chunkSize = NUM_PER_PAGE;
		}
		for (int i = startIndex; i < (startIndex + chunkSize); i++)
		{
			_players.add(players.get(i));
		}
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x36);
		
		writeD(_size);
		writeD(_players.size());
		for (PlayerInstance player : _players)
		{
			writeS(player.getName());
			writeD(player.getClassId().getId());
			writeD(player.getLevel());
			final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
			writeD((world != null) && (world.getTemplateId() >= 0) ? world.getTemplateId() : -1);
			final Map<Integer, Long> _instanceTimes = InstanceManager.getInstance().getAllInstanceTimes(player.getObjectId());
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
