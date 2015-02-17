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

import java.util.ArrayList;
import java.util.List;

import org.l2junity.gameserver.instancemanager.InstanceManager;
import org.l2junity.gameserver.model.PartyMatchWaitingList;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.instancezone.InstanceWorld;

/**
 * @author Gnacik
 */
public class ExListPartyMatchingWaitingRoom extends L2GameServerPacket
{
	private final PlayerInstance _activeChar;
	// private final int _page;
	private final int _minlvl;
	private final int _maxlvl;
	private final int _mode;
	private final int _currentTemplateId;
	private final List<PlayerInstance> _members;
	
	public ExListPartyMatchingWaitingRoom(PlayerInstance player, int page, int minlvl, int maxlvl, int mode)
	{
		_activeChar = player;
		// _page = page;
		_minlvl = minlvl;
		_maxlvl = maxlvl;
		_mode = mode;
		_members = new ArrayList<>();
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		_currentTemplateId = (world != null) && (world.getTemplateId() >= 0) ? world.getTemplateId() : -1;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x36);
		if (_mode == 0)
		{
			writeD(0);
			writeD(0);
			return;
		}
		
		for (PlayerInstance cha : PartyMatchWaitingList.getInstance().getPlayers())
		{
			if ((cha == null) || (cha == _activeChar))
			{
				continue;
			}
			
			if (!cha.isPartyWaiting())
			{
				PartyMatchWaitingList.getInstance().removePlayer(cha);
				continue;
			}
			
			else if ((cha.getLevel() < _minlvl) || (cha.getLevel() > _maxlvl))
			{
				continue;
			}
			
			_members.add(cha);
		}
		
		writeD(0x01); // Page?
		writeD(_members.size());
		for (PlayerInstance member : _members)
		{
			writeS(member.getName());
			writeD(member.getActiveClass());
			writeD(member.getLevel());
			writeD(_currentTemplateId);
			writeD(0x00); // TODO: Instance ID reuse size
			// TODO: Loop for instanceId
		}
	}
}
