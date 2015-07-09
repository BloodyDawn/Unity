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
package org.l2junity.gameserver.handler;

import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.L2TargetType;
import org.l2junity.gameserver.util.Util;

/**
 * @author UnAfraid
 */
public interface ITargetTypeHandler
{
	WorldObject[] EMPTY_TARGET_LIST = new WorldObject[0];
	
	WorldObject[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target);
	
	Enum<L2TargetType> getTargetType();

	default boolean addPet(Creature caster, PlayerInstance owner, int radius, boolean isDead)
	{
		final Summon pet = owner.getPet();
		if (pet == null)
		{
			return false;
		}
		return addCharacter(caster, pet, radius, isDead);
	}

	default boolean addCharacter(Creature caster, Creature target, int radius, boolean isDead)
	{
		if (isDead != target.isDead())
		{
			return false;
		}

		if ((radius > 0) && !Util.checkIfInRange(radius, caster, target, true))
		{
			return false;
		}
		return true;
	}
}
