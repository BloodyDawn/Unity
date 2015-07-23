/*
 * Copyright (C) 2004-2015 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.targethandlers;

import java.util.ArrayList;
import java.util.List;

import org.l2junity.gameserver.handler.ITargetTypeHandler;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.L2TargetType;

/**
 * Target Summon handler.
 * @author UnAfraid
 */
public class Summon implements ITargetTypeHandler
{
	@Override
	public Creature[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		if (activeChar.hasSummon())
		{
			if (!activeChar.hasPet() && activeChar.hasServitors())
			{
				return activeChar.getServitors().values().toArray(new Creature[0]);
			}
			else if (activeChar.hasPet() && !activeChar.hasServitors())
			{
				return new Creature[]
				{
					activeChar.getPet()
				};
			}
			final List<Creature> targets = new ArrayList<>(1 + activeChar.getServitors().size());
			targets.add(activeChar.getPet());
			targets.addAll(activeChar.getServitors().values());
			return targets.toArray(new Creature[0]);
		}
		return EMPTY_TARGET_LIST;
	}
	
	@Override
	public Enum<L2TargetType> getTargetType()
	{
		return L2TargetType.SUMMON;
	}
}
