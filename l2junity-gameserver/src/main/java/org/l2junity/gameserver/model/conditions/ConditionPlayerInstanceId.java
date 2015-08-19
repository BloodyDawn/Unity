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
package org.l2junity.gameserver.model.conditions;

import java.util.ArrayList;

import org.l2junity.gameserver.instancemanager.InstanceManager;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.instancezone.Instance;
import org.l2junity.gameserver.model.items.L2Item;
import org.l2junity.gameserver.model.skills.Skill;

/**
 * The Class ConditionPlayerInstanceId.
 */
public class ConditionPlayerInstanceId extends Condition
{
	private final ArrayList<Integer> _instanceIds;
	
	/**
	 * Instantiates a new condition player instance id.
	 * @param instanceIds the instance ids
	 */
	public ConditionPlayerInstanceId(ArrayList<Integer> instanceIds)
	{
		_instanceIds = instanceIds;
	}
	
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, L2Item item)
	{
		if (effector.getActingPlayer() == null)
		{
			return false;
		}
		
		final int instanceId = effector.getInstanceId();
		if (instanceId <= 0)
		{
			return false; // player not in instance
		}
		
		final Instance instance = InstanceManager.getInstance().getPlayerInstance(effector.getActingPlayer(), true);
		if ((instance == null) || (instance.getId() != instanceId))
		{
			return false; // player in the different instance
		}
		return _instanceIds.contains(instance.getTemplateId());
	}
}
