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
package org.l2junity.gameserver.network.clientpackets;

import org.l2junity.gameserver.enums.MatchingRoomType;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.matching.CommandChannelMatchingRoom;
import org.l2junity.gameserver.model.matching.MatchingRoom;
import org.l2junity.gameserver.network.SystemMessageId;
import org.l2junity.gameserver.network.serverpackets.ExMPCCRoomInfo;

/**
 * @author Sdw
 */
public class RequestExManageMpccRoom extends L2GameClientPacket
{
	private int _roomId;
	private int _maxMembers;
	private int _minLevel;
	private int _maxLevel;
	private String _title;
	
	@Override
	protected void readImpl()
	{
		_roomId = readD();
		_maxMembers = readD();
		_minLevel = readD();
		_maxLevel = readD();
		readD(); // TODO: Find me
		_title = readS();
	}
	
	@Override
	protected void runImpl()
	{
		final PlayerInstance activeChar = getActiveChar();
		
		if (activeChar == null)
		{
			return;
		}
		
		final MatchingRoom room = activeChar.getMatchingRoom();
		
		if ((room == null) || (room.getId() != _roomId) || (room.getRoomType() != MatchingRoomType.COMMAND_CHANNEL) || (room.getLeader() != activeChar))
		{
			return;
		}
		
		room.setTitle(_title);
		room.setMaxMembers(_maxMembers);
		room.setMinLvl(_minLevel);
		room.setMaxLvl(_maxLevel);
		
		room.getMembers().forEach(p ->
		{
			p.sendPacket(new ExMPCCRoomInfo((CommandChannelMatchingRoom) room));
		});
		
		activeChar.sendPacket(SystemMessageId.THE_COMMAND_CHANNEL_MATCHING_ROOM_INFORMATION_WAS_EDITED);
	}
	
	@Override
	public String getType()
	{
		return getClass().getSimpleName();
	}
}
