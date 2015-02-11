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
package instances.FaeronTrainingGrounds1;

import org.l2junity.gameserver.instancemanager.InstanceManager;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.L2PcInstance;
import org.l2junity.gameserver.model.instancezone.InstanceWorld;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.network.NpcStringId;
import org.l2junity.gameserver.network.serverpackets.ExShowScreenMessage;

import instances.AbstractInstance;
import quests.Q10735_ASpecialPower.Q10735_ASpecialPower;

/**
 * Fearon Training Grounds Instance Zone.
 * @author Sdw
 */
public final class FaeronTrainingGrounds1 extends AbstractInstance
{
	// NPC's
	private static final int AYANTHE = 33942;
	private static final int AYANTHE_2 = 33944;
	// Locations
	private static final Location START_LOC = new Location(-74903, 240618, -3584);
	private static final Location EXIT_LOC = new Location(-82088, 249880, -3392);
	// Misc
	private static final int TEMPLATE_ID = 251;
	
	protected class FTGWorld extends InstanceWorld
	{
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, L2PcInstance player)
	{
		final QuestState qs = player.getQuestState(Q10735_ASpecialPower.class.getSimpleName());
		if (qs == null)
		{
			return null;
		}
		
		if (event.equals("enter_instance"))
		{
			enterInstance(player, new FTGWorld(), "FaeronTrainingGrounds1.xml", TEMPLATE_ID);
			
		}
		else if (event.equals("exit_instance"))
		{
			final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
			world.removeAllowed(player.getObjectId());
			teleportPlayer(player, EXIT_LOC, 0);
		}
		
		return super.onAdvEvent(event, npc, player);
	}
	
	public FaeronTrainingGrounds1()
	{
		super(FaeronTrainingGrounds1.class.getSimpleName());
		addStartNpc(AYANTHE, AYANTHE_2);
		addTalkId(AYANTHE, AYANTHE_2);
	}
	
	@Override
	public void onEnterInstance(L2PcInstance player, InstanceWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player.getObjectId());
			showOnScreenMsg(player, NpcStringId.TALK_TO_MAGISTER_AYANTHE, ExShowScreenMessage.TOP_CENTER, 4500);
		}
		teleportPlayer(player, START_LOC, world.getInstanceId());
	}
}