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
package org.l2junity.gameserver.network.serverpackets.friend;

import java.util.Calendar;

import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.instance.L2PcInstance;
import org.l2junity.gameserver.network.serverpackets.L2GameServerPacket;

/**
 * @author Sdw
 */
public class ExFriendDetailInfo extends L2GameServerPacket
{
	private final int _objectId;
	private final L2PcInstance _friend;
	private final String _name;
	private final int _lastAccess;
	
	public ExFriendDetailInfo(L2PcInstance player, String name)
	{
		_objectId = player.getObjectId();
		_name = name;
		_friend = World.getInstance().getPlayer(_name);
		_lastAccess = _friend.isBlocked(player) ? 0 : _friend.isOnline() ? (int) System.currentTimeMillis() : (int) (System.currentTimeMillis() - _friend.getLastAccess()) / 1000;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0xEC);
		
		writeD(_objectId);
		
		if (_friend == null)
		{
			writeS(_name);
			writeD(0);
			writeD(0);
			writeH(0);
			writeH(0);
			writeD(0);
			writeD(0);
			writeS("");
			writeD(0);
			writeD(0);
			writeS("");
			writeD(1);
			writeS(""); // memo
		}
		else
		{
			writeS(_friend.getName());
			writeD(_friend.isOnlineInt());
			writeD(_friend.getObjectId());
			writeH(_friend.getLevel());
			writeH(_friend.getClassId().getId());
			writeD(_friend.getClanId());
			writeD(_friend.getClanCrestId());
			writeS(_friend.getClan() != null ? _friend.getClan().getName() : "");
			writeD(_friend.getAllyId());
			writeD(_friend.getAllyCrestId());
			writeS(_friend.getClan() != null ? _friend.getClan().getAllyName() : "");
			Calendar createDate = _friend.getCreateDate();
			writeC(createDate.get(Calendar.MONTH) + 1);
			writeC(createDate.get(Calendar.DAY_OF_MONTH));
			writeD(_lastAccess);
			writeS(""); // memo
		}
	}
}
