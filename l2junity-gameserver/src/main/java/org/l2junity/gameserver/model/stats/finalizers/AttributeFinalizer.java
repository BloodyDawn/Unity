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

import org.l2junity.gameserver.enums.AttributeType;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.itemcontainer.Inventory;
import org.l2junity.gameserver.model.items.enchant.attribute.AttributeHolder;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.stats.IStatsFunction;
import org.l2junity.gameserver.model.stats.Stats;

/**
 * @author UnAfraid
 */
public class AttributeFinalizer implements IStatsFunction
{
	private final AttributeType _type;
	private final boolean _isWeapon;
	
	public AttributeFinalizer(AttributeType type, boolean isWeapon)
	{
		_type = type;
		_isWeapon = isWeapon;
	}
	
	@Override
	public double calc(Creature creature, double baseValue, Stats stat)
	{
		if (_isWeapon)
		{
			final ItemInstance weapon = creature.getActiveWeaponInstance();
			if (weapon != null)
			{
				final AttributeHolder holder = weapon.getAttribute(_type);
				if (holder != null)
				{
					baseValue += holder.getValue();
				}
			}
		}
		else
		{
			final Inventory inventory = creature.getInventory();
			if (inventory != null)
			{
				for (ItemInstance item : inventory.getItems(ItemInstance::isArmor, ItemInstance::isEquipped))
				{
					final AttributeHolder holder = item.getAttribute(_type);
					if (holder != null)
					{
						baseValue += holder.getValue();
					}
				}
			}
		}
		return Stats.defaultValue(creature, stat, baseValue);
	}
}
