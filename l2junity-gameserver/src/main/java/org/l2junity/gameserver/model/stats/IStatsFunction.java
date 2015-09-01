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
package org.l2junity.gameserver.model.stats;

import java.util.Optional;

import org.l2junity.gameserver.model.PcCondOverride;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.transform.Transform;
import org.l2junity.gameserver.model.items.Weapon;

/**
 * @author UnAfraid
 */
@FunctionalInterface
public interface IStatsFunction
{
	default void throwIfPresent(Optional<Double> base)
	{
		if (base.isPresent())
		{
			throw new IllegalArgumentException("base should not be set for " + getClass().getSimpleName());
		}
	}
	
	default double calcWeaponBaseValue(Creature creature, Stats stat)
	{
		if (creature.isPlayable())
		{
			final Weapon weapon = creature.getActiveWeaponItem();
			final Transform transform = creature.getTransformation();
			final double baseTemplateBalue = creature.getTemplate().getBaseValue(stat, 0);
			return (weapon != null ? weapon.getStats(stat, baseTemplateBalue) : transform != null ? transform.getStats(creature.getActingPlayer(), stat, baseTemplateBalue) : baseTemplateBalue);
		}
		return creature.getTemplate().getBaseValue(stat, 0);
	}
	
	default double validateValue(Creature creature, double value, double maxValue)
	{
		if ((value > maxValue) && !creature.canOverrideCond(PcCondOverride.MAX_STATS_VALUE))
		{
			return maxValue;
		}
		return value;
	}
	
	public double calc(Creature creature, Optional<Double> base, Stats stat);
}
