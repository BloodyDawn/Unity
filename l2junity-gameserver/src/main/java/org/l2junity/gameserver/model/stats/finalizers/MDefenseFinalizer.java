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

import java.util.NoSuchElementException;
import java.util.Optional;

import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.itemcontainer.Inventory;
import org.l2junity.gameserver.model.stats.BaseStats;
import org.l2junity.gameserver.model.stats.IStatsFunction;
import org.l2junity.gameserver.model.stats.Stats;

/**
 * @author UnAfraid
 */
public class MDefenseFinalizer implements IStatsFunction
{
	@Override
	public double calc(Creature creature, Optional<Double> baseValue, Stats stat)
	{
		if (!baseValue.isPresent())
		{
			throw new NoSuchElementException("base value should present!");
		}
		double value = baseValue.get();
		if (creature.isPlayer())
		{
			PlayerInstance p = creature.getActingPlayer();
			if (!p.getInventory().isPaperdollSlotEmpty(Inventory.PAPERDOLL_LFINGER))
			{
				value -= p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, Inventory.PAPERDOLL_LFINGER) : Inventory.PAPERDOLL_LFINGER);
			}
			if (!p.getInventory().isPaperdollSlotEmpty(Inventory.PAPERDOLL_RFINGER))
			{
				value -= p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, Inventory.PAPERDOLL_RFINGER) : Inventory.PAPERDOLL_RFINGER);
			}
			if (!p.getInventory().isPaperdollSlotEmpty(Inventory.PAPERDOLL_LEAR))
			{
				value -= p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, Inventory.PAPERDOLL_LEAR) : Inventory.PAPERDOLL_LEAR);
			}
			if (!p.getInventory().isPaperdollSlotEmpty(Inventory.PAPERDOLL_REAR))
			{
				value -= p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, Inventory.PAPERDOLL_REAR) : Inventory.PAPERDOLL_REAR);
			}
			if (!p.getInventory().isPaperdollSlotEmpty(Inventory.PAPERDOLL_NECK))
			{
				value -= p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, Inventory.PAPERDOLL_NECK) : Inventory.PAPERDOLL_NECK);
			}
			value *= BaseStats.CHA.calcBonus(creature);
		}
		else if (creature.isPet() && (creature.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_NECK) != 0))
		{
			value -= 13;
		}
		value *= BaseStats.MEN.calcBonus(creature) * creature.getLevelMod();
		return Stats.defaultMulValue(creature, stat, value);
	}
}
