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
package org.l2junity.gameserver.model.eventengine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.l2junity.gameserver.ThreadPoolManager;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.sauronsoftware.cron4j.Predictor;

/**
 * @author UnAfraid
 */
public class EventScheduler
{
	private static final Logger LOGGER = LoggerFactory.getLogger(EventScheduler.class);
	private final AbstractEventManager<?> _manager;
	private final String _name;
	private final String _pattern;
	private final boolean _repeat;
	private List<EventMethodNotification> _notifications;
	private ScheduledFuture<?> _task;
	
	public EventScheduler(AbstractEventManager<?> manager, StatsSet set)
	{
		_manager = manager;
		_name = set.getString("name", "");
		_pattern = set.getString("minute", "*") + " " + set.getString("hour", "*") + " " + set.getString("dayOfMonth", "*") + " " + set.getString("month", "*") + " " + set.getString("dayOfWeek", "*");
		_repeat = set.getBoolean("repeat", false);
	}
	
	public String getName()
	{
		return _name;
	}
	
	public long getNextSchedule()
	{
		final Predictor predictor = new Predictor(_pattern);
		return predictor.nextMatchingTime();
	}
	
	public boolean isRepeating()
	{
		return _repeat;
	}
	
	public void addEventNotification(EventMethodNotification notification)
	{
		if (_notifications == null)
		{
			_notifications = new ArrayList<>();
		}
		_notifications.add(notification);
	}
	
	public List<EventMethodNotification> getEventNotifications()
	{
		return _notifications;
	}
	
	public void startScheduler()
	{
		if (_notifications == null)
		{
			LOGGER.info("Scheduler without notificator manager: {} pattern: {}", _manager.getClass().getSimpleName(), _pattern);
			return;
		}
		
		final Predictor predictor = new Predictor(_pattern);
		final long nextSchedule = predictor.nextMatchingTime();
		final long timeSchedule = nextSchedule - System.currentTimeMillis();
		if (timeSchedule <= (30 * 1000))
		{
			LOGGER.warn("Wrong reschedule for {} end up run in {} seconds!", _manager.getClass().getSimpleName(), timeSchedule / 1000);
			ThreadPoolManager.getInstance().scheduleEvent(this::startScheduler, timeSchedule + 1000);
			return;
		}
		
		if (_task != null)
		{
			_task.cancel(false);
		}
		
		_task = ThreadPoolManager.getInstance().scheduleEvent(() ->
		{
			for (EventMethodNotification notification : _notifications)
			{
				try
				{
					notification.execute();
					LOGGER.info("Executed {}#{}", notification.getManager().getClass().getSimpleName(), notification.getMethod().getName());
				}
				catch (Exception e)
				{
					LOGGER.warn("Failed to notify to event manager: {} method: {}", notification.getManager().getClass().getSimpleName(), notification.getMethod().getName());
				}
			}
			
			if (isRepeating())
			{
				ThreadPoolManager.getInstance().scheduleEvent(this::startScheduler, 1000);
			}
		} , timeSchedule);
		
		for (EventMethodNotification notification : _notifications)
		{
			LOGGER.info("Scheduled call to {}#{} on: {}", notification.getManager().getClass().getSimpleName(), notification.getMethod().getName(), Util.formatDate(new Date(nextSchedule), "yyyy-MM-dd HH:mm:ss"));
		}
	}
	
	public void stopScheduler()
	{
		if (_task != null)
		{
			_task.cancel(false);
			_task = null;
		}
	}
	
	public long getRemainingTime(TimeUnit unit)
	{
		return (_task != null) && !_task.isDone() ? _task.getDelay(unit) : 0;
	}
}
