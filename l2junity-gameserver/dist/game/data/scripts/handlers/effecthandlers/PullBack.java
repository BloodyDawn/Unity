/*
 * Copyright (C) 2004-2015 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.effecthandlers;

import org.l2junity.gameserver.GeoData;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.network.client.send.FlyToLocation;
import org.l2junity.gameserver.network.client.send.FlyToLocation.FlyType;
import org.l2junity.gameserver.network.client.send.ValidateLocation;

/**
 * An effect that pulls effected target back to the effector.
 * @author Nik
 */
public final class PullBack extends AbstractEffect
{
	private int _speed = 0;
	private int _delay = 0;
	private int _animationSpeed = 0;
	private FlyType _type;
	
	public PullBack(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		if (params != null)
		{
			_speed = params.getInt("speed", 0);
			_delay = params.getInt("delay", _speed);
			_animationSpeed = params.getInt("animationSpeed", 0);
			_type = params.getEnum("type", FlyType.class, FlyType.WARP_FORWARD); // type 9
		}
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(BuffInfo info)
	{
		final Creature effector = info.getEffector();
		final Creature effected = info.getEffected();
		
		// In retail, you get debuff, but you are not even moved if there is obstacle. You are still disabled from using skills and moving though.
		if (GeoData.getInstance().canMove(effected, effector))
		{
			effected.broadcastPacket(new FlyToLocation(effected, effector, _type, _speed, _delay, _animationSpeed));
			effected.setXYZ(effector);
			effected.broadcastPacket(new ValidateLocation(effected));
		}
	}
}
