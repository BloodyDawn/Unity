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
package ai.npc.ClanHallDoorManager;

import java.util.StringTokenizer;

import org.l2junity.gameserver.model.ClanPrivilege;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.entity.ClanHall;

import ai.npc.AbstractNpcAI;

/**
 * Clan Hall Door Manager AI.
 * @author St3eT
 */
public final class ClanHallDoorManager extends AbstractNpcAI
{
	// NPCs
	// @formatter:off
	private static final int[] DOOR_MANAGERS =
	{
		35450, 35448, 35444, 35442, 35446, 35440, // Aden
		35579, 35577, 35575, 35567, 35573, 35571, 35569, // Rune
		35456, 35452, 35458, 35460, 35454, // Giran
		35468, 35466, 35464, 35462, // Goddard
		35581, 35583, 35585, 35587, // Schuttgart
		35406, 35402, 35404, // Dion
		35385, 35389, 35387, 35391, // Gludio
		36726, 36724, 36722, 36728, // Gludio Outskirts
		35393, 35401, 35399, 35397, 35395, // Gludin
	};
	// @formatter:on
	
	private ClanHallDoorManager()
	{
		super(ClanHallDoorManager.class.getSimpleName(), "ai/npc");
		addStartNpc(DOOR_MANAGERS);
		addTalkId(DOOR_MANAGERS);
		addFirstTalkId(DOOR_MANAGERS);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final StringTokenizer st = new StringTokenizer(event, " ");
		final String action = st.nextToken();
		final ClanHall clanHall = npc.getClanHall();
		String htmltext = null;
		
		if (clanHall != null)
		{
			switch (action)
			{
				case "index":
				{
					htmltext = onFirstTalk(npc, player);
					break;
				}
				case "manageDoors":
				{
					if (isOwningClan(player, npc) && st.hasMoreTokens() && player.hasClanPrivilege(ClanPrivilege.CH_OPEN_DOOR))
					{
						final boolean open = st.nextToken().equals("1");
						clanHall.openCloseDoors(open);
						htmltext = "ClanHallDoorManager-0" + (open ? "5" : "6") + ".html";
					}
					else
					{
						htmltext = "ClanHallDoorManager-04.html";
					}
					break;
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, PlayerInstance player)
	{
		String htmltext = null;
		final ClanHall clanHall = npc.getClanHall();
		if (isOwningClan(player, npc))
		{
			htmltext = getHtm(player.getHtmlPrefix(), "ClanHallDoorManager-01.html");
			htmltext = htmltext.replace("%ownerClanName%", clanHall.getOwner().getName());
		}
		else if (clanHall.getOwnerId() <= 0)
		{
			htmltext = "ClanHallDoorManager-02.html";
		}
		else
		{
			htmltext = getHtm(player.getHtmlPrefix(), "ClanHallDoorManager-03.html");
			htmltext = htmltext.replace("%ownerName%", clanHall.getOwner().getLeaderName());
			htmltext = htmltext.replace("%ownerClanName%", clanHall.getOwner().getName());
		}
		return htmltext;
	}
	
	private boolean isOwningClan(PlayerInstance player, Npc npc)
	{
		return ((npc.getClanHall().getOwnerId() == player.getClanId()) && (player.getClanId() != 0));
	}
	
	public static void main(String[] args)
	{
		new ClanHallDoorManager();
	}
}