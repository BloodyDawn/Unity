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
package ai.npc.Sirra;

import org.l2junity.gameserver.instancemanager.InstanceManager;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.L2PcInstance;
import org.l2junity.gameserver.model.instancezone.InstanceWorld;

import ai.npc.AbstractNpcAI;

/**
 * Sirra AI.
 * @author St3eT
 */
public final class Sirra extends AbstractNpcAI
{
	// NPC
	private static final int SIRRA = 32762;
	// Misc
	private static final int FREYA_INSTID = 139;
	private static final int FREYA_HARD_INSTID = 144;
	
	private Sirra()
	{
		super(Sirra.class.getSimpleName(), "ai/npc");
		addFirstTalkId(SIRRA);
	}
	
	@Override
	public String onFirstTalk(Npc npc, L2PcInstance player)
	{
		final InstanceWorld world = InstanceManager.getInstance().getWorld(npc.getInstanceId());
		
		if ((world != null) && (world.getTemplateId() == FREYA_INSTID))
		{
			return (world.isStatus(0)) ? "32762-easy.html" : "32762-easyfight.html";
		}
		else if ((world != null) && (world.getTemplateId() == FREYA_HARD_INSTID))
		{
			return (world.isStatus(0)) ? "32762-hard.html" : "32762-hardfight.html";
		}
		return "32762.html";
	}
	
	public static void main(String[] args)
	{
		new Sirra();
	}
}