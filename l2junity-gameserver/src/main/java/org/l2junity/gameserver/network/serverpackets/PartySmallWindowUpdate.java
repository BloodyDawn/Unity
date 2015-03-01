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

import org.l2junity.gameserver.enums.PartySmallWindowUpdateType;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.network.PacketWriter;

public final class PartySmallWindowUpdate implements IGameServerPacket
{
	private final PlayerInstance _member;
	private int _flags = 0;
	
	public PartySmallWindowUpdate(PlayerInstance member, boolean addAllFlags)
	{
		_member = member;
		if (addAllFlags)
		{
			for (PartySmallWindowUpdateType type : PartySmallWindowUpdateType.values())
			{
				addUpdateType(type);
			}
		}
	}
	
	public void addUpdateType(PartySmallWindowUpdateType type)
	{
		_flags |= type.getMask();
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.PARTY_SMALL_WINDOW_UPDATE.writeId(packet);
		
		packet.writeD(_member.getObjectId());
		packet.writeH(_flags);
		if (IGameServerPacket.containsMask(_flags, PartySmallWindowUpdateType.CURRENT_CP))
		{
			packet.writeD((int) _member.getCurrentCp()); // c4
		}
		if (IGameServerPacket.containsMask(_flags, PartySmallWindowUpdateType.MAX_CP))
		{
			packet.writeD(_member.getMaxCp()); // c4
		}
		if (IGameServerPacket.containsMask(_flags, PartySmallWindowUpdateType.CURRENT_HP))
		{
			packet.writeD((int) _member.getCurrentHp());
		}
		if (IGameServerPacket.containsMask(_flags, PartySmallWindowUpdateType.MAX_HP))
		{
			packet.writeD(_member.getMaxHp());
		}
		if (IGameServerPacket.containsMask(_flags, PartySmallWindowUpdateType.CURRENT_MP))
		{
			packet.writeD((int) _member.getCurrentMp());
		}
		if (IGameServerPacket.containsMask(_flags, PartySmallWindowUpdateType.MAX_MP))
		{
			packet.writeD(_member.getMaxMp());
		}
		if (IGameServerPacket.containsMask(_flags, PartySmallWindowUpdateType.LEVEL))
		{
			packet.writeC(_member.getLevel());
		}
		if (IGameServerPacket.containsMask(_flags, PartySmallWindowUpdateType.CLASS_ID))
		{
			packet.writeH(_member.getClassId().getId());
		}
		if (IGameServerPacket.containsMask(_flags, PartySmallWindowUpdateType.VITALITY_POINTS))
		{
			packet.writeD(_member.getVitalityPoints());
		}
		return true;
	}
}
