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
package quests.Q10391_ASuspiciousHelper;

import org.l2junity.gameserver.enums.Race;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.quest.Quest;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.model.quest.State;

/**
 * A Suspicious Helper (10391)
 * @author St3eT
 */
public final class Q10391_ASuspiciousHelper extends Quest
{
	// NPCs
	private static final int ELI = 33858;
	private static final int CHEL = 33861;
	private static final int IASON = 33859;
	// Items
	private static final int CARD = 36707; // Forged Identification Card
	private static final int EXP_MATERTIAL = 36708; // Experimental Material
	private static final int STEEL_COIN = 37045; // Steel Door Guild Coin
	private static final int EAC = 952; // Scroll: Enchant Armor (C-grade)
	// Misc
	private static final int MIN_LEVEL = 40;
	private static final int MAX_LEVEL = 46;
	
	public Q10391_ASuspiciousHelper()
	{
		super(10391, Q10391_ASuspiciousHelper.class.getSimpleName(), "A Suspicious Helper");
		addStartNpc(ELI);
		addTalkId(ELI, CHEL, IASON);
		registerQuestItems(CARD);
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "33858-06.htm");
		addCondNotRace(Race.ERTHEIA, "33858-07.htm");
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState st = getQuestState(player, false);
		
		if (st == null)
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "33858-02.htm":
			case "33858-03.htm":
			case "33861-02.html":
			case "33859-02.html":
			case "33859-03.html":
			{
				htmltext = event;
				break;
			}
			case "33858-04.htm":
			{
				giveItems(player, CARD, 1);
				st.startQuest();
				htmltext = event;
				break;
			}
			case "33861-03.html":
			{
				if (st.isCond(1))
				{
					st.setCond(2, true);
					takeItems(player, CARD, -1);
					giveItems(player, EXP_MATERTIAL, 1);
					htmltext = event;
				}
				break;
			}
			case "33859-04.html":
			{
				if (st.isCond(2))
				{
					st.exitQuest(false, true);
					giveItems(player, STEEL_COIN, 1);
					giveItems(player, EAC, 1);
					addExpAndSp(player, 388290, 93);
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED:
			{
				if (npc.getId() == ELI)
				{
					htmltext = "33858-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case ELI:
					{
						if (st.isCond(1))
						{
							htmltext = "33858-05.html";
						}
						break;
					}
					case CHEL:
					{
						if (st.isCond(1))
						{
							htmltext = "33861-01.html";
						}
						else if (st.isCond(2))
						{
							htmltext = "33861-04.html";
						}
						break;
					}
					case IASON:
					{
						if (st.isCond(2))
						{
							htmltext = "33859-01.html";
						}
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				if (npc.getId() == ELI)
				{
					htmltext = getAlreadyCompletedMsg(player);
				}
				break;
			}
		}
		return htmltext;
	}
}