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

import org.l2junity.gameserver.model.Party;
import org.l2junity.gameserver.model.Party.MessageType;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.actor.request.PartyRequest;
import org.l2junity.gameserver.model.matching.MatchingRoom;
import org.l2junity.gameserver.network.L2GameClient;
import org.l2junity.gameserver.network.SystemMessageId;
import org.l2junity.gameserver.network.serverpackets.JoinParty;
import org.l2junity.gameserver.network.serverpackets.SystemMessage;
import org.l2junity.network.PacketReader;

public final class RequestAnswerJoinParty implements IGameClientPacket
{
	private int _response;
	
	@Override
	public boolean read(PacketReader packet)
	{
		_response = packet.readD();
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
		
		final PartyRequest request = player.getRequest(PartyRequest.class);
		if ((request == null) || request.isProcessing() || !player.removeRequest(request.getClass()))
		{
			return;
		}
		request.setProcessing(true);
		
		final PlayerInstance requestor = request.getActiveChar();
		if (requestor == null)
		{
			return;
		}
		
		final Party party = request.getParty();
		final Party requestorParty = requestor.getParty();
		if ((requestorParty != null) && (requestorParty != party))
		{
			return;
		}
		
		requestor.sendPacket(new JoinParty(_response));
		
		if (_response == 1)
		{
			if (party.getMemberCount() >= 7)
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.THE_PARTY_IS_FULL);
				player.sendPacket(sm);
				requestor.sendPacket(sm);
				return;
			}
			
			// Assign the party to the leader upon accept of his partner
			if (requestorParty == null)
			{
				requestor.setParty(party);
			}
			
			player.joinParty(party);
			
			final MatchingRoom requestorRoom = requestor.getMatchingRoom();
			
			if (requestorRoom != null)
			{
				requestorRoom.addMember(player);
			}
		}
		else if (_response == -1)
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_SET_TO_REFUSE_PARTY_REQUESTS_AND_CANNOT_RECEIVE_A_PARTY_REQUEST);
			sm.addPcName(player);
			requestor.sendPacket(sm);
			
			if (party.getMemberCount() == 1)
			{
				party.removePartyMember(requestor, MessageType.NONE);
			}
		}
		else
		{
			if (party.getMemberCount() == 1)
			{
				party.removePartyMember(requestor, MessageType.NONE);
			}
		}
		
		party.setPendingInvitation(false);
		request.setProcessing(false);
	}
}
