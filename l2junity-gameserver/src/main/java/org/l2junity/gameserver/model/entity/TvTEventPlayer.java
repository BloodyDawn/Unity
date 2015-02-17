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
package org.l2junity.gameserver.model.entity;

import org.l2junity.gameserver.model.actor.instance.PlayerInstance;

/**
 * @author UnAfraid
 */
public class TvTEventPlayer
{
	private final PlayerInstance _player;
	
	protected TvTEventPlayer(PlayerInstance player)
	{
		_player = player;
		
	}
	
	public boolean isOnEvent()
	{
		return TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(getPlayer().getObjectId());
	}
	
	public boolean isBlockingExit()
	{
		return true;
	}
	
	public boolean isBlockingDeathPenalty()
	{
		return true;
	}
	
	public boolean canRevive()
	{
		return false;
	}
	
	public PlayerInstance getPlayer()
	{
		return _player;
	}
}
