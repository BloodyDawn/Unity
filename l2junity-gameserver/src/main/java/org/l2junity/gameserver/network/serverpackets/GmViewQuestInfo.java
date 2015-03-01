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
import org.l2junity.gameserver.model.quest.Quest;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.network.PacketWriter;

/**
 * @author Tempy
 */
public class GmViewQuestInfo implements IGameServerPacket
{
	
	private final PlayerInstance _activeChar;
	
	public GmViewQuestInfo(PlayerInstance cha)
	{
		_activeChar = cha;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.GM_VIEW_QUEST_INFO.writeId(packet);
		
		packet.writeS(_activeChar.getName());
		
		Quest[] questList = _activeChar.getAllActiveQuests();
		
		packet.writeH(questList.length); // quest count
		
		for (Quest q : questList)
		{
			final QuestState qs = _activeChar.getQuestState(q.getName());
			
			packet.writeD(q.getId());
			packet.writeD(qs == null ? 0 : qs.getCond());
		}
		packet.writeH(0x00); // some size
		// for size; ddQQ
		return true;
	}
}
