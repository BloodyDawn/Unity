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

import org.l2junity.Config;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.transform.Transform;
import org.l2junity.gameserver.model.items.Weapon;
import org.l2junity.gameserver.model.stats.BaseStats;
import org.l2junity.gameserver.model.stats.IStatsFunction;
import org.l2junity.gameserver.model.stats.Stats;

/**
 * @author UnAfraid
 */
public class MAttackFinalizer implements IStatsFunction
{
	@Override
	public double calc(Creature creature, Optional<Double> base, Stats stat)
	{
		throwIfPresent(base);
		
		float bonusAtk = 1;
		if (Config.L2JMOD_CHAMPION_ENABLE && creature.isChampion())
		{
			bonusAtk = Config.L2JMOD_CHAMPION_ATK;
		}
		if (creature.isRaid())
		{
			bonusAtk *= Config.RAID_MATTACK_MULTIPLIER;
		}
		
		final Weapon weapon = creature.getActiveWeaponItem();
		final Transform transform = creature.getTransformation();
		double baseValue = (weapon != null ? weapon.getStats(stat, 0) : transform != null ? transform.getStats(creature.getActingPlayer(), stat, 0) : creature.getTemplate().getBaseValue(stat, 0)) * bonusAtk;
		
		// Calculate modifiers Magic Attack
		final double chaMod = creature.isPlayer() ? BaseStats.CHA.calcBonus(creature) : 1.;
		baseValue *= Math.pow(BaseStats.INT.calcBonus(creature), 2) * Math.pow(creature.getLevelMod(), 2) * chaMod;
		return Stats.defaultMulValue(creature, stat, baseValue);
	}
}
