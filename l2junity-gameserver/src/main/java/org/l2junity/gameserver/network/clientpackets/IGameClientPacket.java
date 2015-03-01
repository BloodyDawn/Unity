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

import java.util.logging.Logger;

import org.l2junity.gameserver.network.L2GameClient;
import org.l2junity.network.IIncomingPacket;

/**
 * Packets received by the game server from clients
 * @author KenM
 */
public interface IGameClientPacket extends IIncomingPacket<L2GameClient>
{
	static final Logger _log = Logger.getLogger(IGameClientPacket.class.getName());
	
	/**
	 * Overridden with true value on some packets that should disable spawn protection (RequestItemList and UseItem only)
	 * @return
	 */
	public default boolean triggersOnActionRequest()
	{
		return true;
	}
}
