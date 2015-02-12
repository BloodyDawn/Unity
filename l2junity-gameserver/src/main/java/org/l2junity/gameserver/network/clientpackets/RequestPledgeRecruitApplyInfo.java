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

import org.l2junity.gameserver.enums.ClanEntryStatus;
import org.l2junity.gameserver.instancemanager.ClanEntryManager;
import org.l2junity.gameserver.model.actor.instance.L2PcInstance;
import org.l2junity.gameserver.network.serverpackets.ExPledgeRecruitApplyInfo;

/**
 * @author Sdw
 */
public class RequestPledgeRecruitApplyInfo extends L2GameClientPacket
{
	private static final String _C__D0_DE_REQUESTPLEDGERECRUITAPPLYINFO = "[C] D0:DE RequestPledgeRecruitApplyInfo";
	
	@Override
	protected void readImpl()
	{
		
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		ClanEntryStatus status = ClanEntryStatus.DEFAULT;
		
		if ((activeChar.getClan() != null) && activeChar.isClanLeader() && ClanEntryManager.getInstance().isClanRegistred(activeChar.getClanId()))
		{
			status = ClanEntryStatus.ORDERED;
		}
		else if ((activeChar.getClan() == null) && (ClanEntryManager.getInstance().isPlayerRegistred(activeChar.getObjectId())))
		{
			status = ClanEntryStatus.WAITING;
		}
		
		activeChar.sendPacket(new ExPledgeRecruitApplyInfo(status));
	}
	
	@Override
	public String getType()
	{
		return _C__D0_DE_REQUESTPLEDGERECRUITAPPLYINFO;
	}
}
