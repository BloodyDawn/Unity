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

import java.util.LinkedList;
import java.util.List;

import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.base.ClassId;
import org.l2junity.gameserver.network.serverpackets.ExListPartyMatchingWaitingRoom;

/**
 * @author Gnacik
 */
public class RequestListPartyMatchingWaitingRoom extends L2GameClientPacket
{
	private int _page;
	private int _minLevel;
	private int _maxLevel;
	private List<ClassId> _classId; // 1 - waitlist 0 - room waitlist
	private String _query;
	
	@Override
	protected void readImpl()
	{
		_page = readD();
		_minLevel = readD();
		_maxLevel = readD();
		final int size = readD();
		
		if ((size > 0) && (size < 128))
		{
			_classId = new LinkedList<>();
			for (int i = 0; i < size; i++)
			{
				_classId.add(ClassId.getClassId(readD()));
			}
		}
		if (_buf.hasRemaining())
		{
			_query = readS();
		}
	}
	
	@Override
	protected void runImpl()
	{
		final PlayerInstance activeChar = getActiveChar();
		
		if (activeChar == null)
		{
			return;
		}
		
		activeChar.sendPacket(new ExListPartyMatchingWaitingRoom(activeChar, _page, _minLevel, _maxLevel, _classId, _query));
	}
	
	@Override
	public String getType()
	{
		return getClass().getSimpleName();
	}
}