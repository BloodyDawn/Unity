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
package org.l2junity.gameserver.network.serverpackets.sayune;

import org.l2junity.gameserver.enums.SayuneType;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.interfaces.ILocational;
import org.l2junity.gameserver.network.serverpackets.L2GameServerPacket;

/**
 * @author UnAfraid
 */
public class ExFlyMoveBroadcast extends L2GameServerPacket
{
	private final int _objectId;
	private final int _mapId;
	private final ILocational _currentLoc;
	private final ILocational _targetLoc;
	private final SayuneType _type;
	
	public ExFlyMoveBroadcast(PlayerInstance activeChar, SayuneType type, int mapId, ILocational targetLoc)
	{
		_objectId = activeChar.getObjectId();
		_type = type;
		_mapId = mapId;
		_currentLoc = activeChar;
		_targetLoc = targetLoc;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x108);
		writeD(_objectId);
		
		writeD(_type.ordinal());
		writeD(_mapId);
		
		writeLoc(_targetLoc);
		writeD(0x00); // ?
		writeLoc(_currentLoc);
	}
}
