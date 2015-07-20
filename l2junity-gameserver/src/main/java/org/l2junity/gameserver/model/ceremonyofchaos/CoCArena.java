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
package org.l2junity.gameserver.model.ceremonyofchaos;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2junity.gameserver.instancemanager.InstanceManager;
import org.l2junity.gameserver.model.actor.instance.L2MonsterInstance;
import org.l2junity.gameserver.model.entity.Instance;

/**
 * @author UnAfraid
 */
public class CoCArena
{
	private final int _id;
	private final Instance _instance;
	private CoCArenaState _state = CoCArenaState.PREPARING;
	private final Set<CoCPlayer> _players = ConcurrentHashMap.newKeySet();
	private final Set<L2MonsterInstance> _monsters = ConcurrentHashMap.newKeySet();
	
	public CoCArena(int id, String instanceTemplate)
	{
		_id = id;
		_instance = InstanceManager.getInstance().getInstance(InstanceManager.getInstance().createDynamicInstance(instanceTemplate));
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getInstanceId()
	{
		return _instance.getObjectId();
	}
	
	public Instance getInstance()
	{
		return _instance;
	}
	
	public CoCArenaState getState()
	{
		return _state;
	}
	
	public void setState(CoCArenaState state)
	{
		_state = state;
	}

	public void addPlayer(CoCPlayer player)
	{
		_players.add(player);
	}
	
	public Set<CoCPlayer> getPlayers()
	{
		return _players;
	}
	
	public Set<L2MonsterInstance> getMonsters()
	{
		return _monsters;
	}
}
