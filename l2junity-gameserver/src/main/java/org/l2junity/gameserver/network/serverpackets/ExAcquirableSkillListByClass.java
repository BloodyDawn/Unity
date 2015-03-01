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

import java.util.List;

import org.l2junity.gameserver.model.SkillLearn;
import org.l2junity.gameserver.model.base.AcquireSkillType;
import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.network.PacketWriter;

/**
 * @author UnAfraid
 */
public class ExAcquirableSkillListByClass implements IGameServerPacket
{
	final List<SkillLearn> _learnable;
	final AcquireSkillType _type;
	
	public ExAcquirableSkillListByClass(List<SkillLearn> learnable, AcquireSkillType type)
	{
		_learnable = learnable;
		_type = type;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_ACQUIRABLE_SKILL_LIST_BY_CLASS.writeId(packet);
		
		packet.writeH(_type.getId());
		packet.writeH(_learnable.size());
		for (SkillLearn skill : _learnable)
		{
			packet.writeD(skill.getSkillId());
			packet.writeH(skill.getSkillLevel());
			packet.writeH(skill.getSkillLevel());
			packet.writeC(skill.getGetLevel());
			packet.writeQ(skill.getLevelUpSp());
			packet.writeC(skill.getRequiredItems().size());
			if (_type == AcquireSkillType.SUBPLEDGE)
			{
				packet.writeH(0x00);
			}
		}
		return true;
	}
}
