/*
 * Copyright (C) 2004-2016 L2J Unity
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
package handlers.effecthandlers;

import java.util.concurrent.atomic.AtomicBoolean;

import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.stats.Stats;

/**
 * @author UnAfraid
 */
public abstract class AbstractConditionalEffect extends AbstractStatEffect
{
	private EffectedConditionHolder _holder;
	
	protected AbstractConditionalEffect(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params, Stats stat)
	{
		super(attachCond, applyCond, set, params, stat);
	}
	
	@Override
	public final void onStart(BuffInfo info)
	{
		_holder = new EffectedConditionHolder(info, canPump(info));
	}
	
	@Override
	public final void onExit(BuffInfo info)
	{
		unregisterCondition(info);
	}
	
	protected EffectedConditionHolder getHolder()
	{
		return _holder;
	}
	
	protected abstract void registerCondition(BuffInfo info);
	
	protected abstract void unregisterCondition(BuffInfo info);
	
	protected final void update()
	{
		final BuffInfo info = _holder.getInfo();
		final boolean condStatus = canPump(info);
		if (_holder.getLastConditionStatus().compareAndSet(!condStatus, condStatus))
		{
			info.getEffected().getStat().recalculateStats(true);
		}
	}
	
	protected static class EffectedConditionHolder
	{
		private final BuffInfo _info;
		private final AtomicBoolean _lastConditionStatus = new AtomicBoolean();
		
		public EffectedConditionHolder(BuffInfo info, boolean condStatus)
		{
			_info = info;
			_lastConditionStatus.set(condStatus);
		}
		
		public BuffInfo getInfo()
		{
			return _info;
		}
		
		public AtomicBoolean getLastConditionStatus()
		{
			return _lastConditionStatus;
		}
	}
}
