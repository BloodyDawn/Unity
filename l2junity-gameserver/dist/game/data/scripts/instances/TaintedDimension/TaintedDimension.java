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
package instances.TaintedDimension;

import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.instancezone.Instance;

import instances.AbstractInstance;

/**
 * Tainted Dimension instance zone. TODO: Finish me!
 * @author St3eT
 */
public final class TaintedDimension extends AbstractInstance
{
	// Misc
	private static final int TEMPLATE_ID = 192;
	
	public TaintedDimension()
	{
	
	}
	
	@Override
	public void onTimerEvent(String event, StatsSet params, Npc npc, PlayerInstance player)
	{
		final Instance instance = npc != null ? npc.getInstanceWorld() : player.getInstanceWorld();
		if (instance != null)
		{
			switch (event)
			{
				case "INSTANCE_END":
				{
					instance.finishInstance(0);
					break;
				}
			}
		}
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		switch (event)
		{
			case "enterInstance":
			{
				enterInstance(player, npc, TEMPLATE_ID);
				break;
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	protected void onEnter(PlayerInstance player, Instance instance, boolean firstEnter)
	{
		super.onEnter(player, instance, firstEnter);
		getTimers().addTimer("INSTANCE_END", 10000, null, player);
	}
	
	public static void main(String[] args)
	{
		new TaintedDimension();
	}
}
