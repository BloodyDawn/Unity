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
import org.l2junity.gameserver.model.Party;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.matching.MatchingRoom;
import org.l2junity.gameserver.network.SystemMessageId;

/**
 * format (ch) d
 * @author -Wooden-
 */
public final class RequestOustFromPartyRoom extends L2GameClientPacket
{
	private int _charObjId;
	
	@Override
	protected void readImpl()
	{
		_charObjId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		PlayerInstance player = getActiveChar();
		if (player == null)
		{
			return;
		}
		
		PlayerInstance member = World.getInstance().getPlayer(_charObjId);
		if (member == null)
		{
			return;
		}
		
		final MatchingRoom room = player.getMatchingRoom();
		if ((room == null) || (room.getRoomType() != MatchingRoomType.PARTY) || (room.getLeader() != player) || (player == member))
		{
			return;
		}
		
		final Party playerParty = player.getParty();
		final Party memberParty = player.getParty();
		
		if ((playerParty != null) && (memberParty != null) && (playerParty.getLeaderObjectId() == memberParty.getLeaderObjectId()))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DISMISS_A_PARTY_MEMBER_BY_FORCE);
		}
		else
		{
			room.deleteMember(member, true);
		}
	}
	
	@Override
	public String getType()
	{
		return getClass().getSimpleName();
	}
}
