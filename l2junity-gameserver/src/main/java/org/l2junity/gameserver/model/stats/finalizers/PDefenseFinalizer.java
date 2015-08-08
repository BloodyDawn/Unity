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
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.itemcontainer.Inventory;
import org.l2junity.gameserver.model.items.L2Item;
import org.l2junity.gameserver.model.stats.BaseStats;
import org.l2junity.gameserver.model.stats.IStatsFunction;
import org.l2junity.gameserver.model.stats.Stats;

/**
 * @author UnAfraid
 */
public class PDefenseFinalizer implements IStatsFunction
{
	@Override
	public double calc(Creature creature, Optional<Double> base, Stats stat)
	{
		double value = 0;
		if (base.isPresent())
		{
			value = base.get();
		}
		if (creature.isPlayer())
		{
			final PlayerInstance p = creature.getActingPlayer();
			if (!p.getInventory().isPaperdollSlotEmpty(Inventory.PAPERDOLL_CHEST))
			{
				value -= p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, Inventory.PAPERDOLL_CHEST) : p.getTemplate().getBaseDefBySlot(Inventory.PAPERDOLL_CHEST);
			}
			if (!p.getInventory().isPaperdollSlotEmpty(Inventory.PAPERDOLL_LEGS) || (!p.getInventory().isPaperdollSlotEmpty(Inventory.PAPERDOLL_CHEST) && (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR)))
			{
				value -= p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, Inventory.PAPERDOLL_LEGS) : Inventory.PAPERDOLL_LEGS);
			}
			if (!p.getInventory().isPaperdollSlotEmpty(Inventory.PAPERDOLL_HEAD))
			{
				value -= p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, Inventory.PAPERDOLL_HEAD) : Inventory.PAPERDOLL_HEAD);
			}
			if (!p.getInventory().isPaperdollSlotEmpty(Inventory.PAPERDOLL_FEET))
			{
				value -= p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, Inventory.PAPERDOLL_FEET) : Inventory.PAPERDOLL_FEET);
			}
			if (!p.getInventory().isPaperdollSlotEmpty(Inventory.PAPERDOLL_GLOVES))
			{
				value -= p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, Inventory.PAPERDOLL_GLOVES) : Inventory.PAPERDOLL_GLOVES);
			}
			if (!p.getInventory().isPaperdollSlotEmpty(Inventory.PAPERDOLL_UNDER))
			{
				value -= p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, Inventory.PAPERDOLL_UNDER) : Inventory.PAPERDOLL_UNDER);
			}
			if (!p.getInventory().isPaperdollSlotEmpty(Inventory.PAPERDOLL_CLOAK))
			{
				value -= p.getTemplate().getBaseDefBySlot(p.isTransformed() ? p.getTransformation().getBaseDefBySlot(p, Inventory.PAPERDOLL_CLOAK) : Inventory.PAPERDOLL_CLOAK);
			}
			
			value *= BaseStats.CHA.calcBonus(p);
		}
		value *= creature.getLevelMod();
		return Stats.defaultMulValue(creature, stat, value);
	}
}
