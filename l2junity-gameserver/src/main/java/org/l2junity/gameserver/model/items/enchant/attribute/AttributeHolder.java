/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver.model.items.enchant.attribute;

import java.util.concurrent.atomic.AtomicBoolean;

import org.l2junity.gameserver.enums.AttributeType;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.stats.Stats;
import org.l2junity.gameserver.model.stats.functions.FuncAdd;

/**
 * @author UnAfraid
 */
public class AttributeHolder
{
	private final AttributeType _type;
	private int _value;
	private final AtomicBoolean _isActive = new AtomicBoolean(false);
	
	public AttributeHolder(AttributeType type, int value)
	{
		_type = type;
		_value = value;
	}
	
	public AttributeType getType()
	{
		return _type;
	}
	
	public int getValue()
	{
		return _value;
	}
	
	public void setValue(int value)
	{
		_value = value;
	}
	
	public void incValue(int with)
	{
		_value += with;
	}
	
	public void apply(PlayerInstance player, boolean isArmor)
	{
		if (!_isActive.compareAndSet(false, true))
		{
			return;
		}
		
		switch (_type)
		{
			case FIRE:
			{
				player.addStatFunc(new FuncAdd(isArmor ? Stats.FIRE_RES : Stats.FIRE_POWER, 0x40, this, _value, null));
				break;
			}
			case WATER:
			{
				player.addStatFunc(new FuncAdd(isArmor ? Stats.WATER_RES : Stats.WATER_POWER, 0x40, this, _value, null));
				break;
			}
			case WIND:
			{
				player.addStatFunc(new FuncAdd(isArmor ? Stats.WIND_RES : Stats.WIND_POWER, 0x40, this, _value, null));
				break;
			}
			case EARTH:
			{
				player.addStatFunc(new FuncAdd(isArmor ? Stats.EARTH_RES : Stats.EARTH_POWER, 0x40, this, _value, null));
				break;
			}
			case DARK:
			{
				player.addStatFunc(new FuncAdd(isArmor ? Stats.DARK_RES : Stats.DARK_POWER, 0x40, this, _value, null));
				break;
			}
			case HOLY:
			{
				player.addStatFunc(new FuncAdd(isArmor ? Stats.HOLY_RES : Stats.HOLY_POWER, 0x40, this, _value, null));
				break;
			}
		}
	}
	
	public void remove(PlayerInstance player)
	{
		if (!_isActive.compareAndSet(true, false))
		{
			return;
		}
		
		player.removeStatsOwner(this);
	}
	
	@Override
	public String toString()
	{
		return _type.name() + " +" + _value;
	}
}
