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

import org.l2junity.gameserver.enums.ExManagePartyRoomMemberType;
import org.l2junity.gameserver.enums.MatchingMemberType;
import org.l2junity.gameserver.instancemanager.MapRegionManager;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.matching.CommandChannelMatchingRoom;

/**
 * @author Gnacik
 */
public class ExManageMpccRoomMember extends L2GameServerPacket
{
	private final PlayerInstance _activeChar;
	private final MatchingMemberType _memberType;
	private final ExManagePartyRoomMemberType _type;
	
	public ExManageMpccRoomMember(PlayerInstance player, CommandChannelMatchingRoom room, ExManagePartyRoomMemberType mode)
	{
		_activeChar = player;
		_memberType = room.getMemberType(player);
		_type = mode;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x0A);
		writeD(_type.ordinal());
		writeD(_activeChar.getObjectId());
		writeS(_activeChar.getName());
		writeD(_activeChar.getClassId().getId());
		writeD(_activeChar.getLevel());
		writeD(MapRegionManager.getInstance().getBBs(_activeChar.getLocation()));
		writeD(_memberType.ordinal());
	}
}
