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
package org.l2junity.gameserver.model.stats.finalizers;

import java.util.Optional;

import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.stats.IStatsFunction;
import org.l2junity.gameserver.model.stats.Stats;

/**
 * @author UnAfraid
 */
public class PAccuracyFinalizer implements IStatsFunction
{
	@Override
	public double calc(Creature creature, Optional<Double> baseValue, Stats stat)
	{
		double value = 0;
		if (baseValue.isPresent())
		{
			value = baseValue.get();
		}
		
		// [Square(DEX)] * 5 + lvl + weapon hitbonus;
		final int level = creature.getLevel();
		value += (Math.sqrt(creature.getDEX()) * 5) + level;
		if (level > 69)
		{
			value += level - 69;
		}
		if (level > 77)
		{
			value += 1;
		}
		if (level > 80)
		{
			value += 2;
		}
		if (level > 87)
		{
			value += 2;
		}
		if (level > 92)
		{
			value += 1;
		}
		if (level > 97)
		{
			value += 1;
		}
		return Stats.defaultValue(creature, stat, value);
	}
	
}
