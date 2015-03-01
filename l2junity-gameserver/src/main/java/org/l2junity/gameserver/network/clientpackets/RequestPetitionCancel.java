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

import org.l2junity.Config;
import org.l2junity.gameserver.data.xml.impl.AdminData;
import org.l2junity.gameserver.enums.ChatType;
import org.l2junity.gameserver.instancemanager.PetitionManager;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.L2GameClient;
import org.l2junity.gameserver.network.SystemMessageId;
import org.l2junity.gameserver.network.serverpackets.CreatureSay;
import org.l2junity.gameserver.network.serverpackets.SystemMessage;
import org.l2junity.network.PacketReader;

/**
 * <p>
 * Format: (c) d
 * <ul>
 * <li>d: Unknown</li>
 * </ul>
 * </p>
 * @author -Wooden-, TempyIncursion
 */
public final class RequestPetitionCancel implements IGameClientPacket
{
	
	// private int _unknown;
	
	@Override
	public boolean read(PacketReader packet)
	{
		// _unknown = packet.readD(); This is pretty much a trigger packet.
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
		
		if (PetitionManager.getInstance().isPlayerInConsultation(activeChar))
		{
			if (activeChar.isGM())
			{
				PetitionManager.getInstance().endActivePetition(activeChar);
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.YOUR_PETITION_IS_BEING_PROCESSED);
			}
		}
		else
		{
			if (PetitionManager.getInstance().isPlayerPetitionPending(activeChar))
			{
				if (PetitionManager.getInstance().cancelActivePetition(activeChar))
				{
					int numRemaining = Config.MAX_PETITIONS_PER_PLAYER - PetitionManager.getInstance().getPlayerTotalPetitionCount(activeChar);
					
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.THE_PETITION_WAS_CANCELED_YOU_MAY_SUBMIT_S1_MORE_PETITION_S_TODAY);
					sm.addString(String.valueOf(numRemaining));
					activeChar.sendPacket(sm);
					
					// Notify all GMs that the player's pending petition has been cancelled.
					String msgContent = activeChar.getName() + " has canceled a pending petition.";
					AdminData.getInstance().broadcastToGMs(new CreatureSay(activeChar.getObjectId(), ChatType.HERO_VOICE, "Petition System", msgContent));
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.FAILED_TO_CANCEL_PETITION_PLEASE_TRY_AGAIN_LATER);
				}
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.YOU_HAVE_NOT_SUBMITTED_A_PETITION);
			}
		}
	}
}
