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

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.events.AbstractScript;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.ListenerRegisterType;
import org.l2junity.gameserver.model.events.annotations.RegisterEvent;
import org.l2junity.gameserver.model.events.annotations.RegisterType;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerLogout;

/**
 * @author UnAfraid
 * @param <T>
 */
public abstract class AbstractEventManager<T extends AbstractEvent<?>> extends AbstractScript
{
	private final StatsSet _variables = new StatsSet();
	private final Set<EventScheduler> _schedulers = new LinkedHashSet<>();
	private final Set<T> _events = ConcurrentHashMap.newKeySet();
	private final Queue<PlayerInstance> _registeredPlayers = new ConcurrentLinkedDeque<>();
	private IEventState _state;
	
	public abstract void onInitialized();
	
	public StatsSet getVariables()
	{
		return _variables;
	}
	
	public Set<EventScheduler> getSchedulers()
	{
		return _schedulers;
	}
	
	public Set<T> getEvents()
	{
		return _events;
	}
	
	public void startScheduler()
	{
		_schedulers.forEach(EventScheduler::startScheduler);
	}
	
	public void stopScheduler()
	{
		_schedulers.forEach(EventScheduler::stopScheduler);
	}
	
	public IEventState getState()
	{
		return _state;
	}
	
	public void setState(IEventState state)
	{
		_state = state;
	}
	
	public final boolean registerPlayer(PlayerInstance player)
	{
		return canRegister(player, true) && _registeredPlayers.offer(player);
	}
	
	public final boolean unregisterPlayer(PlayerInstance player)
	{
		return _registeredPlayers.remove(player);
	}
	
	public final boolean isRegistered(PlayerInstance player)
	{
		return _registeredPlayers.contains(player);
	}

	public boolean canRegister(PlayerInstance player, boolean sendMessage)
	{
		return !_registeredPlayers.contains(player);
	}
	
	public final Queue<PlayerInstance> getRegisteredPlayers()
	{
		return _registeredPlayers;
	}

	@RegisterEvent(EventType.ON_PLAYER_LOGOUT)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	private void onPlayerLogout(OnPlayerLogout event)
	{
		final PlayerInstance player = event.getActiveChar();
		if (_registeredPlayers.remove(player))
		{
			onUnregisteredPlayer(player);
		}
	}
	
	/**
	 * Triggered when a player is automatically removed from the event manager because he disconnected
	 * @param player
	 */
	protected void onUnregisteredPlayer(PlayerInstance player)
	{
		
	}

	@Override
	public String getScriptName()
	{
		return getClass().getSimpleName();
	}
	
	@Override
	public Path getScriptPath()
	{
		return null;
	}
}
