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

import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.ListenersContainer;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureHpChange;
import org.l2junity.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.stats.Stats;

/**
 * @author UnAfraid
 */
public abstract class AbstractConditionalHpEffect extends AbstractConditionalEffect
{
	private final int _hpPercent;
	
	protected AbstractConditionalHpEffect(StatsSet params, Stats stat)
	{
		super(params, stat);
		_hpPercent = params.getInt("hpPercent", -1);
	}
	
	@Override
	protected void registerCondition(BuffInfo info)
	{
		if (_hpPercent > 0)
		{
			final ListenersContainer container = info.getEffected();
			container.addListener(new ConsumerEventListener(container, EventType.ON_CREATURE_HP_CHANGE, (OnCreatureHpChange event) -> onHpChange(event), this));
		}
	}
	
	@Override
	protected void unregisterCondition(BuffInfo info)
	{
		info.getEffected().removeListenerIf(listener -> listener.getOwner() == this);
	}
	
	@Override
	public boolean canPump(BuffInfo info)
	{
		return (_hpPercent <= 0) || (info.getEffected().getCurrentHpPercent() <= _hpPercent);
	}
	
	private void onHpChange(OnCreatureHpChange event)
	{
		final boolean condStatus = event.getCreature().getCurrentHpPercent() <= _hpPercent;
		final EffectedConditionHolder holder = getHolder();
		if (holder.getLastConditionStatus().compareAndSet(!condStatus, condStatus))
		{
			update();
		}
	}
}
