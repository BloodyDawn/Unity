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
package org.l2junity.gameserver.network.client.send;

import java.util.Arrays;
import java.util.List;

import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.network.client.OutgoingPackets;
import org.l2junity.network.PacketWriter;

/**
 * MagicSkillLaunched server packet implementation.
 * @author UnAfraid
 */
public class MagicSkillLaunched implements IClientOutgoingPacket
{
	private final int _charObjId;
	private final int _skillId;
	private final int _skillLevel;
	private final List<WorldObject> _targets;
	
	public MagicSkillLaunched(Creature cha, int skillId, int skillLevel, WorldObject... targets)
	{
		_charObjId = cha.getObjectId();
		_skillId = skillId;
		_skillLevel = skillLevel;
		
		//@formatter:off
		if (targets == null)
		{
			targets = new WorldObject[] { cha };
		}
		//@formatter:on
		_targets = Arrays.asList(targets);
	}
	
	public MagicSkillLaunched(Creature cha, int skillId, int skillLevel)
	{
		this(cha, skillId, skillId, cha);
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.MAGIC_SKILL_LAUNCHED.writeId(packet);
		
		packet.writeD(0x00); // TODO: Find me!
		packet.writeD(_charObjId);
		packet.writeD(_skillId);
		packet.writeD(_skillLevel);
		packet.writeD(_targets.size());
		for (WorldObject target : _targets)
		{
			packet.writeD(target.getObjectId());
		}
		return true;
	}
}
