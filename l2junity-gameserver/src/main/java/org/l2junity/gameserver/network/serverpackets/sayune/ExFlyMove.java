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

import java.util.List;

import org.l2junity.gameserver.enums.SayuneType;
import org.l2junity.gameserver.model.SayuneEntry;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.serverpackets.L2GameServerPacket;

/**
 * @author UnAfraid
 */
public class ExFlyMove extends L2GameServerPacket
{
	private final int _objectId;
	private final SayuneType _type;
	private final int _mapId;
	private final List<SayuneEntry> _locations;
	
	public ExFlyMove(PlayerInstance activeChar, SayuneType type, int mapId, List<SayuneEntry> locations)
	{
		_objectId = activeChar.getObjectId();
		_type = type;
		_mapId = mapId;
		_locations = locations;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0xE8);
		writeD(_objectId);
		
		writeD(_type.ordinal());
		writeD(0x00); // ??
		writeD(_mapId);
		
		writeD(_locations.size());
		for (SayuneEntry loc : _locations)
		{
			writeD(loc.getId());
			writeD(0x00); // ??
			writeLoc(loc);
		}
	}
}
