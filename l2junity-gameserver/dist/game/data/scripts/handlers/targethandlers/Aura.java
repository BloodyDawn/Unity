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
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.L2DoorInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.L2TargetType;
import org.l2junity.gameserver.model.zone.ZoneId;

/**
 * Aura target handler.
 * @author UnAfraid
 */
public class Aura implements ITargetTypeHandler
{
	@Override
	public Creature[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		final List<Creature> targetList = new ArrayList<>();
		final boolean srcInArena = (activeChar.isInsideZone(ZoneId.PVP) && !activeChar.isInsideZone(ZoneId.SIEGE));
		for (Creature obj : World.getInstance().getVisibleObjects(activeChar, Creature.class, skill.getAffectRange()))
		{
			if (obj.isDoor() || obj.isAttackable() || obj.isPlayable())
			{
				// Stealth door targeting.
				if (obj.isDoor())
				{
					final L2DoorInstance door = (L2DoorInstance) obj;
					if (!door.getTemplate().isStealth())
					{
						continue;
					}
				}
				
				if (!Skill.checkForAreaOffensiveSkills(activeChar, obj, skill, srcInArena))
				{
					continue;
				}
				
				if (activeChar.isPlayable() && obj.isAttackable() && !skill.isBad())
				{
					continue;
				}
				
				if (onlyFirst)
				{
					return new Creature[]
					{
						obj
					};
				}
				
				targetList.add(obj);
			}
		}
		return targetList.toArray(new Creature[targetList.size()]);
	}
	
	@Override
	public Enum<L2TargetType> getTargetType()
	{
		return L2TargetType.AURA;
	}
}
