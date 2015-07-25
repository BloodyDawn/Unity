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
package org.l2junity.gameserver.model.events.impl.character;

import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.impl.IBaseEvent;

/**
 * @author Nik
 */
public class OnCreatureTeleport implements IBaseEvent
{
	private final Creature _creature;
	private final int _destX;
	private final int _destY;
	private final int _destZ;
	private final int _destHeading;
	private final int _destInstanceId;
	private final int _randomOffset;
	
	public OnCreatureTeleport(Creature creature, int destX, int destY, int destZ, int destHeading, int destInstanceId, int randomOffset)
	{
		_creature = creature;
		_destX = destX;
		_destY = destY;
		_destZ = destZ;
		_destHeading = destHeading;
		_destInstanceId = destInstanceId;
		_randomOffset = randomOffset;
	}
	
	public Creature getCreature()
	{
		return _creature;
	}
	
	public int getDestX()
	{
		return _destX;
	}
	
	public int getDestY()
	{
		return _destY;
	}
	
	public int getDestZ()
	{
		return _destZ;
	}
	
	public int getDestHeading()
	{
		return _destHeading;
	}
	
	public int getDestInstanceId()
	{
		return _destInstanceId;
	}
	
	public int getRandomOffset()
	{
		return _randomOffset;
	}
	
	@Override
	public EventType getType()
	{
		return EventType.ON_CREATURE_TELEPORT;
	}
}