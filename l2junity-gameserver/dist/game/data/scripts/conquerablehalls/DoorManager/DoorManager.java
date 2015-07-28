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
package conquerablehalls.DoorManager;

import java.util.StringTokenizer;

import org.l2junity.gameserver.model.ClanPrivilege;
import org.l2junity.gameserver.model.PcCondOverride;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;

import ai.npc.AbstractNpcAI;

/**
 * Door Manager AI.
 * @author St3eT
 */
public final class DoorManager extends AbstractNpcAI
{
	// NPCs
	// @formatter:off
	private static final int[] DOORMENS =
	{
		30596, // Fortress of Resistance
		35417, 35418, // Devastated Castle
		35433, 35434, 35435, 35436, // Bandit Stronghold
		35601, 35602, // Rainbow Springs Chateau 
		35641, 35642, // Forrest of the Dead

	};
	// @formatter:on
	
	private DoorManager()
	{
		super(DoorManager.class.getSimpleName(), "conquerablehalls");
		addStartNpc(DOORMENS);
		addFirstTalkId(DOORMENS);
		addTalkId(DOORMENS);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final StringTokenizer st = new StringTokenizer(event, " ");
		final String action = st.nextToken();
		
		if (action.equals("open_doors"))
		{
			if (isOwningClan(player, npc))
			{
				if (isSiegeInProgress(npc))
				{
					return npc.getId() + "-busy.htm";
				}
				
				while (st.hasMoreTokens())
				{
					npc.getConquerableHall().openCloseDoor(Integer.parseInt(st.nextToken()), true);
				}
			}
		}
		else if (action.equals("close_doors"))
		{
			if (isOwningClan(player, npc))
			{
				if (isSiegeInProgress(npc))
				{
					return npc.getId() + "-busy.htm";
				}
				
				while (st.hasMoreTokens())
				{
					npc.getConquerableHall().openCloseDoor(Integer.parseInt(st.nextToken()), false);
				}
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onFirstTalk(Npc npc, PlayerInstance player)
	{
		return isOwningClan(player, npc) && player.hasClanPrivilege(ClanPrivilege.CS_OPEN_DOOR) ? npc.getId() + ".htm" : npc.getId() + "-no.htm";
	}
	
	private boolean isOwningClan(PlayerInstance player, Npc npc)
	{
		return player.canOverrideCond(PcCondOverride.CASTLE_CONDITIONS) || ((player.getClan() != null) && (player.getClanId() == npc.getConquerableHall().getOwnerId()) && player.isClanLeader());
	}
	
	private boolean isSiegeInProgress(Npc npc)
	{
		return (npc.getConquerableHall() != null) && npc.getConquerableHall().isInSiege();
	}
	
	public static void main(String[] args)
	{
		new DoorManager();
	}
}