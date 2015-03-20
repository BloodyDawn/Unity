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
package handlers.effecthandlers;

import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.skills.BuffInfo;

/**
 * Untargetable effect implementation.
 * @author UnAfraid
 */
public final class Untargetable extends AbstractEffect
{
	public Untargetable(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public boolean canStart(BuffInfo info)
	{
		return info.getEffected().isPlayer();
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		// Set untargetable
		info.getEffected().setTargetable(false);
		
		// Remove target from those that have the untargetable creature on target.
		World.getInstance().forEachVisibleObject(info.getEffected(), Creature.class, c ->
		{
			if (c.getTarget() == info.getEffected())
			{
				c.setTarget(null);
			}
		});
	}
	
	@Override
	public void onExit(BuffInfo info)
	{
		info.getEffected().setTargetable(true);
	}
}
