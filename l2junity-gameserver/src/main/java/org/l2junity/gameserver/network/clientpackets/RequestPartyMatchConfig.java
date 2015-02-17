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

import org.l2junity.gameserver.model.PartyMatchRoom;
import org.l2junity.gameserver.model.PartyMatchRoomList;
import org.l2junity.gameserver.model.PartyMatchWaitingList;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.SystemMessageId;
import org.l2junity.gameserver.network.serverpackets.ActionFailed;
import org.l2junity.gameserver.network.serverpackets.ExPartyRoomMember;
import org.l2junity.gameserver.network.serverpackets.ListPartyWating;
import org.l2junity.gameserver.network.serverpackets.PartyMatchDetail;

/**
 * This class ...
 * @version $Revision: 1.1.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestPartyMatchConfig extends L2GameClientPacket
{
	private static final String _C__7F_REQUESTPARTYMATCHCONFIG = "[C] 7F RequestPartyMatchConfig";
	
	private int _auto, _loc, _lvl;
	
	@Override
	protected void readImpl()
	{
		_auto = readD(); //
		_loc = readD(); // Location
		_lvl = readD(); // my level
	}
	
	@Override
	protected void runImpl()
	{
		PlayerInstance _activeChar = getClient().getActiveChar();
		
		if (_activeChar == null)
		{
			return;
		}
		
		if (!_activeChar.isInPartyMatchRoom() && (_activeChar.getParty() != null) && (_activeChar.getParty().getLeader() != _activeChar))
		{
			_activeChar.sendPacket(SystemMessageId.THE_LIST_OF_PARTY_ROOMS_CAN_ONLY_BE_VIEWED_BY_A_PERSON_WHO_IS_NOT_PART_OF_A_PARTY);
			_activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (_activeChar.isInPartyMatchRoom())
		{
			// If Player is in Room show him room, not list
			PartyMatchRoomList _list = PartyMatchRoomList.getInstance();
			if (_list == null)
			{
				return;
			}
			
			PartyMatchRoom _room = _list.getPlayerRoom(_activeChar);
			if (_room == null)
			{
				return;
			}
			
			_activeChar.sendPacket(new PartyMatchDetail(_activeChar, _room));
			_activeChar.sendPacket(new ExPartyRoomMember(_activeChar, _room, 2));
			
			_activeChar.setPartyRoom(_room.getId());
			// _activeChar.setPartyMatching(1);
			_activeChar.broadcastUserInfo();
		}
		else
		{
			// Add to waiting list
			PartyMatchWaitingList.getInstance().addPlayer(_activeChar);
			
			// Send Room list
			ListPartyWating matchList = new ListPartyWating(_activeChar, _auto, _loc, _lvl);
			
			_activeChar.sendPacket(matchList);
		}
	}
	
	@Override
	public String getType()
	{
		return _C__7F_REQUESTPARTYMATCHCONFIG;
	}
}
