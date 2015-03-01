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

import org.l2junity.gameserver.data.sql.impl.ClanTable;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.L2GameClient;
import org.l2junity.gameserver.network.SystemMessageId;
import org.l2junity.network.PacketReader;

/**
 * This class ...
 * @version $Revision: 1.4.2.1.2.3 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestReplyStartPledgeWar implements IGameClientPacket
{
	private int _answer;
	
	@Override
	public boolean read(PacketReader packet)
	{
		packet.readS();
		_answer = packet.readD();
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		PlayerInstance activeChar = client.getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		PlayerInstance requestor = activeChar.getActiveRequester();
		if (requestor == null)
		{
			return;
		}
		
		if (_answer == 1)
		{
			ClanTable.getInstance().storeclanswars(requestor.getClanId(), activeChar.getClanId());
		}
		else
		{
			requestor.sendPacket(SystemMessageId.THE_S1_CLAN_DID_NOT_RESPOND_WAR_PROCLAMATION_HAS_BEEN_REFUSED2);
		}
		activeChar.setActiveRequester(null);
		requestor.onTransactionResponse();
	}
}
