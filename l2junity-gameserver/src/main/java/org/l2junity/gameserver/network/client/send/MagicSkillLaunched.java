/*
 * Copyright (C) 2004-2015 L2J Unity
 * 
 * This file is part of L2J Unity.
 * 
 * L2J Unity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Unity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver.network.client.send;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.skills.SkillCastingType;
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
	private final SkillCastingType _castingType;
	private final Collection<WorldObject> _targets;
	
	public MagicSkillLaunched(Creature cha, int skillId, int skillLevel, SkillCastingType castingType, Collection<WorldObject> targets)
	{
		_charObjId = cha.getObjectId();
		_skillId = skillId;
		_skillLevel = skillLevel;
		_castingType = castingType;
		
		if (targets == null)
		{
			targets = Collections.singletonList(cha);
		}
		
		_targets = targets;
	}
	
	public MagicSkillLaunched(Creature cha, int skillId, int skillLevel, SkillCastingType castingType, WorldObject... targets)
	{
		this(cha, skillId, skillLevel, castingType, (targets == null ? Collections.singletonList(cha) : Arrays.asList(targets)));
	}
	
	public MagicSkillLaunched(Creature cha, int skillId, int skillLevel)
	{
		this(cha, skillId, skillId, SkillCastingType.NORMAL, Collections.singletonList(cha));
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.MAGIC_SKILL_LAUNCHED.writeId(packet);
		
		packet.writeD(_castingType.getClientBarId()); // MagicSkillUse castingType
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
