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
package org.l2junity.gameserver.model.events;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.events.timers.IEventTimer;
import org.l2junity.gameserver.model.events.timers.TimerHolder;

/**
 * @author UnAfraid
 * @param <T>
 */
public final class TimerExecutor<T>
{
	private final Map<T, Set<TimerHolder<T>>> _timers = new ConcurrentHashMap<>();
	private final IEventTimer<T> _listener;
	
	public TimerExecutor(IEventTimer<T> listener)
	{
		_listener = listener;
	}
	
	/**
	 * Adds timer
	 * @param holder
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	public boolean addTimer(TimerHolder<T> holder)
	{
		return _timers.computeIfAbsent(holder.getEvent(), key -> ConcurrentHashMap.newKeySet()).add(holder);
	}
	
	/**
	 * Adds non-repeating timer (Lambda is supported on the last parameter)
	 * @param event
	 * @param params
	 * @param time
	 * @param npc
	 * @param player
	 * @param eventTimer
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	public boolean addTimer(T event, StatsSet params, long time, Npc npc, PlayerInstance player, IEventTimer<T> eventTimer)
	{
		return addTimer(new TimerHolder<>(event, params, time, npc, player, false, eventTimer, this));
	}
	
	/**
	 * Adds non-repeating timer (Lambda is supported on the last parameter)
	 * @param event
	 * @param time
	 * @param eventTimer
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	public boolean addTimer(T event, long time, IEventTimer<T> eventTimer)
	{
		return addTimer(new TimerHolder<>(event, null, time, null, null, false, eventTimer, this));
	}
	
	/**
	 * Adds non-repeating timer
	 * @param event
	 * @param params
	 * @param time
	 * @param npc
	 * @param player
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	public boolean addTimer(T event, StatsSet params, long time, Npc npc, PlayerInstance player)
	{
		return addTimer(event, params, time, npc, player, _listener);
	}
	
	/**
	 * Adds non-repeating timer
	 * @param event
	 * @param time
	 * @param npc
	 * @param player
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	public boolean addTimer(T event, long time, Npc npc, PlayerInstance player)
	{
		return addTimer(event, null, time, npc, player, _listener);
	}
	
	/**
	 * Adds repeating timer (Lambda is supported on the last parameter)
	 * @param event
	 * @param params
	 * @param time
	 * @param npc
	 * @param player
	 * @param eventTimer
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	public boolean addRepeatingTimer(T event, StatsSet params, long time, Npc npc, PlayerInstance player, IEventTimer<T> eventTimer)
	{
		return addTimer(new TimerHolder<>(event, params, time, npc, player, true, eventTimer, this));
	}
	
	/**
	 * Adds repeating timer
	 * @param event
	 * @param params
	 * @param time
	 * @param npc
	 * @param player
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	public boolean addRepeatingTimer(T event, StatsSet params, long time, Npc npc, PlayerInstance player)
	{
		return addRepeatingTimer(event, params, time, npc, player, _listener);
	}
	
	/**
	 * Adds repeating timer
	 * @param event
	 * @param time
	 * @param npc
	 * @param player
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	public boolean addRepeatingTimer(T event, long time, Npc npc, PlayerInstance player)
	{
		return addRepeatingTimer(event, null, time, npc, player, _listener);
	}
	
	/**
	 * That method is executed right after notification to {@link IEventTimer#onTimerEvent(TimerHolder)} in order to remove the holder from the _timers map
	 * @param holder
	 */
	public void onTimerPostExecute(TimerHolder<T> holder)
	{
		// Remove non repeating timer upon execute
		if (!holder.isRepeating())
		{
			final Set<TimerHolder<T>> timers = _timers.get(holder.getEvent());
			if ((timers == null) || timers.isEmpty())
			{
				return;
			}
			
			// Remove the timer
			timers.removeIf(holder::equals);
			
			// If there's no events inside that set remove it
			if (timers.isEmpty())
			{
				_timers.remove(holder.getEvent());
			}
		}
	}
	
	/**
	 * Cancels and removes all timers from the _timers map
	 */
	public void cancelAllTimers()
	{
		_timers.values().stream().flatMap(Set::stream).forEach(TimerHolder::cancelTimer);
		_timers.clear();
	}
	
	/**
	 * @param event
	 * @param npc
	 * @param player
	 * @return {@code true} if there is a timer with the given event npc and player parameters, {@code false} otherwise
	 */
	public boolean hasTimer(T event, Npc npc, PlayerInstance player)
	{
		final Set<TimerHolder<T>> timers = _timers.get(event);
		if ((timers == null) || timers.isEmpty())
		{
			return false;
		}
		
		return timers.stream().anyMatch(holder -> holder.isEqual(event, npc, player));
	}
	
	/**
	 * @param event
	 * @return {@code true} if there is at least one timer with the given event, {@code false} otherwise
	 */
	public boolean hasTimers(T event)
	{
		return _timers.containsKey(event);
	}
	
	/**
	 * @param event
	 * @return {@code true} if at least one timer for the given event were stopped, {@code false} otherwise
	 */
	public boolean cancelTimers(T event)
	{
		final Set<TimerHolder<T>> timers = _timers.remove(event);
		if ((timers == null) || timers.isEmpty())
		{
			return false;
		}
		
		timers.forEach(TimerHolder::cancelTimer);
		return true;
	}
	
	/**
	 * @param event
	 * @param npc
	 * @param player
	 * @return {@code true} if timer for the given event, npc, player were stopped, {@code false} otheriwse
	 */
	public boolean cancelTimer(T event, Npc npc, PlayerInstance player)
	{
		final Set<TimerHolder<T>> timers = _timers.get(event);
		if ((timers == null) || timers.isEmpty())
		{
			return false;
		}
		
		final Iterator<TimerHolder<T>> holders = timers.iterator();
		while (holders.hasNext())
		{
			final TimerHolder<T> holder = holders.next();
			if (holder.isEqual(event, npc, player))
			{
				holders.remove();
				
				// Remove empty set
				if (timers.isEmpty())
				{
					_timers.remove(event);
				}
				
				return holder.cancelTimer();
			}
		}
		
		return false;
	}
	
	/**
	 * @param event
	 * @param npc
	 * @param player
	 * @return the remaining time of the timer, or -1 in case it doesn't exists
	 */
	public long getRemainingTime(T event, Npc npc, PlayerInstance player)
	{
		final Set<TimerHolder<T>> timers = _timers.get(event);
		if ((timers == null) || timers.isEmpty())
		{
			return -1;
		}
		
		final Iterator<TimerHolder<T>> holders = timers.iterator();
		while (holders.hasNext())
		{
			final TimerHolder<T> holder = holders.next();
			if (holder.isEqual(event, npc, player))
			{
				holders.remove();
				return holder.getRemainingTime();
			}
		}
		
		return -1;
	}
}
