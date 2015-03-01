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

import org.l2junity.gameserver.enums.HtmlActionScope;
import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.network.PacketWriter;

/**
 * NpcQuestHtmlMessage server packet implementation.
 * @author HorridoJoho
 */
public final class NpcQuestHtmlMessage extends AbstractHtmlPacket
{
	private final int _questId;
	
	public NpcQuestHtmlMessage(int npcObjId, int questId)
	{
		super(npcObjId);
		_questId = questId;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_NPC_QUEST_HTML_MESSAGE.writeId(packet);
		
		packet.writeD(getNpcObjId());
		packet.writeS(getHtml());
		packet.writeD(_questId);
		return true;
	}
	
	@Override
	public HtmlActionScope getScope()
	{
		return HtmlActionScope.NPC_QUEST_HTML;
	}
}
