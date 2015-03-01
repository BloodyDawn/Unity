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

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.l2junity.Config;
import org.l2junity.gameserver.enums.PrivateStoreType;
import org.l2junity.gameserver.instancemanager.AntiFeedManager;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.L2GameClient;
import org.l2junity.gameserver.network.SystemMessageId;
import org.l2junity.gameserver.network.client.ConnectionState;
import org.l2junity.gameserver.network.serverpackets.CharSelectionInfo;
import org.l2junity.gameserver.network.serverpackets.RestartResponse;
import org.l2junity.gameserver.taskmanager.AttackStanceTaskManager;
import org.l2junity.network.PacketReader;

/**
 * This class ...
 * @version $Revision: 1.11.2.1.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestRestart implements IGameClientPacket
{
	protected static final Logger _logAccounting = Logger.getLogger("accounting");
	
	@Override
	public boolean read(PacketReader packet)
	{
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		final PlayerInstance player = client.getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (player.hasItemRequest())
		{
			client.sendPacket(RestartResponse.valueOf(false));
			return;
		}
		
		if (player.isLocked())
		{
			_log.warning("Player " + player.getName() + " tried to restart during class change.");
			client.sendPacket(RestartResponse.valueOf(false));
			return;
		}
		
		if (player.getPrivateStoreType() != PrivateStoreType.NONE)
		{
			player.sendMessage("Cannot restart while trading");
			client.sendPacket(RestartResponse.valueOf(false));
			return;
		}
		
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(player) && !(player.isGM() && Config.GM_RESTART_FIGHTING))
		{
			if (Config.DEBUG)
			{
				_log.fine("Player " + player.getName() + " tried to logout while fighting.");
			}
			
			player.sendPacket(SystemMessageId.YOU_CANNOT_RESTART_WHILE_IN_COMBAT);
			client.sendPacket(RestartResponse.valueOf(false));
			return;
		}
		
		if (player.isBlockedFromExit())
		{
			client.sendPacket(RestartResponse.valueOf(false));
			return;
		}
		
		LogRecord record = new LogRecord(Level.INFO, "Logged out");
		record.setParameters(new Object[]
		{
			client
		});
		_logAccounting.log(record);
		
		// detach the client from the char so that the connection isnt closed in the deleteMe
		player.setClient(null);
		
		player.deleteMe();
		
		client.setActiveChar(null);
		AntiFeedManager.getInstance().onDisconnect(client);
		
		// return the client to the authed status
		client.setConnectionState(ConnectionState.AUTHENTICATED);
		
		client.sendPacket(RestartResponse.valueOf(true));
		
		// send char list
		final CharSelectionInfo cl = new CharSelectionInfo(client.getAccountName(), client.getSessionId().playOkID1);
		client.sendPacket(cl);
		client.setCharSelection(cl.getCharInfo());
	}
}
