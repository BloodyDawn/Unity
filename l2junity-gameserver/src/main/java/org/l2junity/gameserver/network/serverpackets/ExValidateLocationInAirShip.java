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

import org.l2junity.gameserver.model.actor.instance.PlayerInstance;

/**
 * update 27.8.10
 * @author kerberos JIV
 */
public class ExValidateLocationInAirShip extends L2GameServerPacket
{
	private final PlayerInstance _activeChar;
	private final int shipId, x, y, z, h;
	
	public ExValidateLocationInAirShip(PlayerInstance player)
	{
		_activeChar = player;
		shipId = _activeChar.getAirShip().getObjectId();
		x = player.getInVehiclePosition().getX();
		y = player.getInVehiclePosition().getY();
		z = player.getInVehiclePosition().getZ();
		h = player.getHeading();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xFE);
		writeH(0x70);
		writeD(_activeChar.getObjectId());
		writeD(shipId);
		writeD(x);
		writeD(y);
		writeD(z);
		writeD(h);
	}
}
