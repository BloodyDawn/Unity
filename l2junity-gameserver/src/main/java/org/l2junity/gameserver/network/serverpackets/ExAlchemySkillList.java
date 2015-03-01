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

import org.l2junity.gameserver.data.xml.impl.SkillTreesData;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.network.PacketWriter;

/**
 * @author UnAfraid
 */
public class ExAlchemySkillList implements IGameServerPacket
{
	private final List<Skill> _skills = new ArrayList<>();
	
	public ExAlchemySkillList(final PlayerInstance player)
	{
		for (Skill skill : player.getAllSkills())
		{
			// Make sure its alchemy skill.
			if (SkillTreesData.getInstance().getAlchemySkill(skill.getId(), skill.getLevel()) != null)
			{
				_skills.add(skill);
			}
		}
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_ALCHEMY_SKILL_LIST.writeId(packet);
		
		packet.writeD(_skills.size());
		for (Skill skill : _skills)
		{
			packet.writeD(skill.getId());
			packet.writeD(skill.getLevel());
			packet.writeQ(0x00); // Always 0 on Naia, SP i guess?
			packet.writeC(0x01); // Always 1 on Naia
		}
		return true;
	}
}
