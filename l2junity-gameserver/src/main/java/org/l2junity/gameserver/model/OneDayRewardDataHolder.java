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
package org.l2junity.gameserver.model;

import java.util.ArrayList;
import java.util.List;

import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.base.ClassId;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.holders.ItemHolder;

/**
 * @author Sdw
 */
public class OneDayRewardDataHolder
{
	private final int _id;
	private final int _rewardId;
	private final List<ItemHolder> _rewardsItems;
	private final List<ClassId> _classRestriction;
	private final int _requiredCompletions;
	private List<Condition> _preCondition;
	
	public OneDayRewardDataHolder(StatsSet set)
	{
		_id = set.getInt("id");
		_rewardId = set.getInt("reward_id");
		_requiredCompletions = set.getInt("requiredCompletions", 0);
		_rewardsItems = set.getList("items", ItemHolder.class);
		_classRestriction = set.getList("classRestriction", ClassId.class);
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getRewardId()
	{
		return _rewardId;
	}
	
	public List<ItemHolder> getRewardsItems()
	{
		return _rewardsItems;
	}
	
	public List<ClassId> getClassRestriction()
	{
		return _classRestriction;
	}
	
	public boolean isAllowedClass(ClassId c)
	{
		return _classRestriction.isEmpty() || _classRestriction.contains(c);
	}
	
	public void attach(Condition c)
	{
		if (_preCondition == null)
		{
			_preCondition = new ArrayList<>();
		}
		_preCondition.add(c);
	}
	
	public boolean canBeClaimed(Creature activeChar)
	{
		if ((_preCondition == null) || _preCondition.isEmpty())
		{
			return true;
		}
		
		for (Condition cond : _preCondition)
		{
			if (!cond.test(activeChar, this))
			{
				return false;
			}
		}
		return true;
	}
	
	public int getRequiredCompletions()
	{
		return _requiredCompletions;
	}
}
