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
package ai.spawns;

import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.spawns.SpawnGroup;
import org.l2junity.gameserver.model.spawns.SpawnTemplate;

import ai.npc.AbstractNpcAI;

/**
 * @author UnAfraid
 */
public final class NoRandomActivity extends AbstractNpcAI
{
	private NoRandomActivity()
	{
		super(NoRandomActivity.class.getSimpleName(), "ai/spawns");
	}
	
	@Override
	public void onSpawnNpc(SpawnTemplate template, SpawnGroup group, Npc npc)
	{
		npc.setRandomAnimationEnabled(npc.getParameters().getBoolean("disableRandomAnimation", false));
		npc.setIsNoRndWalk(npc.getParameters().getBoolean("disableRandomWalk", false));
		if (npc.getSpawn() != null)
		{
			npc.getSpawn().setIsNoRndWalk(npc.getParameters().getBoolean("disableRandomWalk", false));
		}
	}
	
	public static void main(String[] args)
	{
		new NoRandomActivity();
	}
}