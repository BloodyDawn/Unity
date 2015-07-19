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
package org.l2junity.gameserver.model.multisell;

import org.l2junity.gameserver.enums.AttributeType;
import org.l2junity.gameserver.model.items.instance.ItemInstance;

/**
 * @author DS
 */
public class ItemInfo
{
	private final int _enchantLevel, _augmentId;
	private final byte _elementId;
	private final int _elementPower;
	private final int[] _elementals = new int[6];
	
	public ItemInfo(ItemInstance item)
	{
		_enchantLevel = item.getEnchantLevel();
		_augmentId = item.getAugmentation() != null ? item.getAugmentation().getAugmentationId() : 0;
		_elementId = item.getAttackAttributeType().getClientId();
		_elementPower = item.getAttackAttributePower();
		_elementals[0] = item.getDefenceAttribute(AttributeType.FIRE);
		_elementals[1] = item.getDefenceAttribute(AttributeType.WATER);
		_elementals[2] = item.getDefenceAttribute(AttributeType.WIND);
		_elementals[3] = item.getDefenceAttribute(AttributeType.EARTH);
		_elementals[4] = item.getDefenceAttribute(AttributeType.HOLY);
		_elementals[5] = item.getDefenceAttribute(AttributeType.DARK);
	}
	
	public ItemInfo(int enchantLevel)
	{
		_enchantLevel = enchantLevel;
		_augmentId = 0;
		_elementId = AttributeType.NONE.getClientId();
		_elementPower = 0;
		_elementals[0] = 0;
		_elementals[1] = 0;
		_elementals[2] = 0;
		_elementals[3] = 0;
		_elementals[4] = 0;
		_elementals[5] = 0;
	}
	
	public final int getEnchantLevel()
	{
		return _enchantLevel;
	}
	
	public final int getAugmentId()
	{
		return _augmentId;
	}
	
	public final byte getElementId()
	{
		return _elementId;
	}
	
	public final int getElementPower()
	{
		return _elementPower;
	}
	
	public final int[] getElementals()
	{
		return _elementals;
	}
}