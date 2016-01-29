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

import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.network.client.send.StatusUpdate;

/**
 * Cp Heal Over Time effect implementation.
 */
public final class CpHealOverTime extends AbstractEffect
{
	private final double _power;
	
	public CpHealOverTime(StatsSet params)
	{
		_power = params.getDouble("power", 0);
		setTicks(params.getInt("ticks"));
	}
	
	@Override
	public boolean onActionTime(BuffInfo info)
	{
		if (info.getEffected().isDead())
		{
			return false;
		}
		
		double cp = info.getEffected().getCurrentCp();
		double maxcp = info.getEffected().getMaxRecoverableCp();
		
		// Not needed to set the CP and send update packet if player is already at max CP
		if (cp >= maxcp)
		{
			return false;
		}
		
		cp += _power * getTicksMultiplier();
		cp = Math.min(cp, maxcp);
		info.getEffected().setCurrentCp(cp, false);
		final StatusUpdate su = new StatusUpdate(info.getEffected());
		su.addAttribute(StatusUpdate.CUR_CP, (int) cp);
		su.addCaster(info.getEffector());
		info.getEffected().broadcastPacket(su);
		return true;
	}
}
