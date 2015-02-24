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

import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.matching.MatchingRoom;
import org.l2junity.gameserver.network.SystemMessageId;

/**
 * Format: (ch) d
 * @author -Wooden-, Tryskell
 */
public final class AnswerJoinPartyRoom extends L2GameClientPacket
{
	private boolean _answer;
	
	@Override
	protected void readImpl()
	{
		_answer = readD() == 1;
	}
	
	@Override
	protected void runImpl()
	{
		final PlayerInstance player = getActiveChar();
		if (player == null)
		{
			return;
		}
		
		PlayerInstance partner = player.getActiveRequester();
		if (partner == null)
		{
			player.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
			player.setActiveRequester(null);
			return;
		}
		
		if (_answer && !partner.isRequestExpired())
		{
			final MatchingRoom room = partner.getMatchingRoom();
			if (room == null)
			{
				return;
			}
			
			room.addMember(player);
		}
		else
		{
			partner.sendPacket(SystemMessageId.THE_RECIPIENT_OF_YOUR_INVITATION_DID_NOT_ACCEPT_THE_PARTY_MATCHING_INVITATION);
		}
		
		// reset transaction timers
		player.setActiveRequester(null);
		partner.onTransactionResponse();
	}
	
	@Override
	public String getType()
	{
		return AnswerJoinPartyRoom.class.getSimpleName();
	}
}
