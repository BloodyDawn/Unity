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
package ai.npc.ClanHallManager;

import java.util.StringTokenizer;

import org.l2junity.gameserver.model.ClanPrivilege;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.entity.ClanHall;

import ai.npc.AbstractNpcAI;

/**
 * Clan Hall Manager AI.
 * @author St3eT
 */
public final class ClanHallManager extends AbstractNpcAI
{
	// NPCs
	// @formatter:off
	private static final int[] CLANHALL_MANAGERS =
	{
		35384, 35386, 35388, // Gludio
		35455, 35453, 35451, 35457, 35459, // Giran
		35441, 35439, 35443, 35447, 35449, 35445, // Aden
		35467, 35465, 35463, 35461, // Goddard
		35578, 35576, 35574, 35566, 35572, 35570, 35568, // Rune
		35407, 35405, 35403, // Dion
		35394, 35396, 35398, 35400, 35392, // Gludin
	};
	// @formatter:on
	
	private ClanHallManager()
	{
		super(ClanHallManager.class.getSimpleName(), "ai/npc");
		addStartNpc(CLANHALL_MANAGERS);
		addTalkId(CLANHALL_MANAGERS);
		addFirstTalkId(CLANHALL_MANAGERS);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final StringTokenizer st = new StringTokenizer(event, " ");
		final String action = st.nextToken();
		final ClanHall clanHall = npc.getClanHall();
		String htmltext = null;
		
		if ((clanHall != null) && isOwningClan(player, npc))
		{
			switch (action)
			{
				case "index":
				{
					htmltext = isOwningClan(player, npc) ? "ClanHallManager-01.html" : "ClanHallManager-03.html";
					break;
				}
				case "manageDoors":
				{
					if (player.hasClanPrivilege(ClanPrivilege.CH_OPEN_DOOR))
					{
						if (st.hasMoreTokens())
						{
							final boolean open = st.nextToken().equals("1");
							clanHall.openCloseDoors(open);
							htmltext = "ClanHallManager-0" + (open ? "5" : "6") + ".html";
						}
						else
						{
							htmltext = "ClanHallManager-04.html";
						}
					}
					else
					{
						htmltext = "ClanHallManager-noAuthority.html";
					}
					break;
				}
				case "expel":
				{
					if (player.hasClanPrivilege(ClanPrivilege.CH_DISMISS))
					{
						if (st.hasMoreTokens())
						{
							clanHall.banishOthers();
							htmltext = "ClanHallManager-08.html";
						}
						else
						{
							htmltext = "ClanHallManager-07.html";
						}
					}
					else
					{
						htmltext = "ClanHallManager-noAuthority.html";
					}
					break;
				}
				default:
				{
					player.sendMessage("This case is not implemented yet. :(");
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
			if (clanHall.getCostFailDay() == 0)
			{
				htmltext = "ClanHallManager-01.html";
			}
			else
			{
				htmltext = getHtm(player.getHtmlPrefix(), "ClanHallManager-02.html");
				htmltext = htmltext.replaceAll("%costFailDayLeft%", Integer.toString((8 - clanHall.getCostFailDay())));
			}
		}
		else
		{
			htmltext = "ClanHallManager-03.html";
		}
		return htmltext;
	}
	
	private boolean isOwningClan(PlayerInstance player, Npc npc)
	{
		return ((npc.getClanHall().getOwnerId() == player.getClanId()) && (player.getClanId() != 0));
	}
	
	public static void main(String[] args)
	{
		new ClanHallManager();
	}
}