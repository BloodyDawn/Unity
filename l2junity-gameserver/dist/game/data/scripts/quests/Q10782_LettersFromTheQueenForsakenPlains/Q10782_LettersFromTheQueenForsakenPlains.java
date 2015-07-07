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
package quests.Q10782_LettersFromTheQueenForsakenPlains;

import org.l2junity.gameserver.enums.HtmlActionScope;
import org.l2junity.gameserver.enums.Race;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.ListenerRegisterType;
import org.l2junity.gameserver.model.events.annotations.RegisterEvent;
import org.l2junity.gameserver.model.events.annotations.RegisterType;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerBypass;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerLevelChanged;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerLogin;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerPressTutorialMark;
import org.l2junity.gameserver.model.quest.Quest;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.network.client.send.ExShowScreenMessage;
import org.l2junity.gameserver.network.client.send.PlaySound;
import org.l2junity.gameserver.network.client.send.TutorialCloseHtml;
import org.l2junity.gameserver.network.client.send.TutorialShowHtml;
import org.l2junity.gameserver.network.client.send.TutorialShowQuestionMark;
import org.l2junity.gameserver.network.client.send.string.NpcStringId;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * Letters from the Queen: Forsaken Plains (10782)
 * @author malyelfik
 */
public final class Q10782_LettersFromTheQueenForsakenPlains extends Quest
{
	// NPCs
	private static final int ORVEN = 30857;
	private static final int NOVAIN = 33866;
	// Items
	private static final int SOE_ADEN = 39576;
	private static final int SOE_FORSAKEN_PLAINS = 39577;
	private static final int ENCHANT_ARMOR_B = 948;
	private static final int STEEL_DOOR_GUILD_COIN = 37045;
	// Location
	private static final Location TELEPORT_LOC = new Location(147446, 22761, -1984);
	// Misc
	private static final int MIN_LEVEL = 58;
	private static final int MAX_LEVEL = 60;
	
	public Q10782_LettersFromTheQueenForsakenPlains()
	{
		super(10782, Q10782_LettersFromTheQueenForsakenPlains.class.getSimpleName(), "Letters from the Queen: Forsaken Plains");
		addTalkId(ORVEN, NOVAIN);
		
		addCondRace(Race.ERTHEIA, "");
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "");
		registerQuestItems(SOE_ADEN, SOE_FORSAKEN_PLAINS);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = event;
		switch (event)
		{
			case "30857-02.html":
			case "33866-02.html":
				break;
			case "30857-03.html":
			{
				if (qs.isCond(1))
				{
					qs.setCond(2, true);
					giveItems(player, SOE_FORSAKEN_PLAINS, 1);
				}
				break;
			}
			case "33866-03.html":
			{
				if (qs.isCond(2))
				{
					giveItems(player, ENCHANT_ARMOR_B, 10);
					giveItems(player, STEEL_DOOR_GUILD_COIN, 60);
					addExpAndSp(player, 731010, 175);
					showOnScreenMsg(player, NpcStringId.GROW_STRONGER_HERE_UNTIL_YOU_RECEIVE_THE_NEXT_LETTER_FROM_QUEEN_NAVARI_AT_LV_61, ExShowScreenMessage.TOP_CENTER, 8000);
					qs.exitQuest(false, true);
				}
				break;
			}
			default:
				htmltext = null;
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, false);
		String htmltext = getNoQuestMsg(player);
		if (qs == null)
		{
			return htmltext;
		}
		
		if (qs.isStarted())
		{
			if (npc.getId() == ORVEN)
			{
				htmltext = (qs.isCond(1)) ? "30857-01.html" : "30857-04.html";
			}
			else if (qs.isCond(2))
			{
				htmltext = "33866-01.html";
			}
		}
		return htmltext;
	}
	
	@RegisterEvent(EventType.ON_PLAYER_PRESS_TUTORIAL_MARK)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerPressTutorialMark(OnPlayerPressTutorialMark event)
	{
		if (event.getMarkId() == getId())
		{
			final PlayerInstance player = event.getActiveChar();
			final QuestState st = getQuestState(player, true);
			
			st.startQuest();
			player.sendPacket(new PlaySound(3, "Npcdialog1.serenia_quest_6", 0, 0, 0, 0, 0));
			player.sendPacket(new TutorialShowHtml(getHtm(player.getHtmlPrefix(), "popup.html")));
			giveItems(player, SOE_ADEN, 1);
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_BYPASS)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerBypass(OnPlayerBypass event)
	{
		final String command = event.getCommand();
		final PlayerInstance player = event.getActiveChar();
		final QuestState st = getQuestState(player, false);
		
		if (command.equals("Q10782_teleport") && (st != null) && st.isCond(1) && hasQuestItems(player, SOE_ADEN))
		{
			if (player.isTransformed())
			{
				showOnScreenMsg(player, NpcStringId.YOU_CANNOT_TELEPORT_WHILE_IN_A_TRANSFORMED_STATE, ExShowScreenMessage.TOP_CENTER, 5000);
			}
			else if (player.isInCombat())
			{
				showOnScreenMsg(player, NpcStringId.YOU_CANNOT_TELEPORT_IN_COMBAT, ExShowScreenMessage.TOP_CENTER, 5000);
			}
			else
			{
				player.teleToLocation(TELEPORT_LOC);
				takeItems(player, SOE_ADEN, -1);
			}
			player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
			player.clearHtmlActions(HtmlActionScope.TUTORIAL_HTML);
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LEVEL_CHANGED)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerLevelChanged(OnPlayerLevelChanged event)
	{
		final PlayerInstance player = event.getActiveChar();
		final QuestState st = getQuestState(player, false);
		final int oldLevel = event.getOldLevel();
		final int newLevel = event.getNewLevel();
		
		if ((st == null) && (player.getRace().equals(Race.ERTHEIA)) && (oldLevel < newLevel) && ((newLevel >= MIN_LEVEL) && (newLevel <= MAX_LEVEL)))
		{
			showOnScreenMsg(player, NpcStringId.QUEEN_NAVARI_HAS_SENT_A_LETTER_NCLICK_THE_QUESTION_MARK_ICON_TO_READ, ExShowScreenMessage.TOP_CENTER, 10000);
			player.sendPacket(new TutorialShowQuestionMark(getId()));
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerLogin(OnPlayerLogin event)
	{
		final PlayerInstance player = event.getActiveChar();
		final QuestState st = getQuestState(player, false);
		
		if ((st == null) && player.getRace().equals(Race.ERTHEIA) && (player.getLevel() >= MIN_LEVEL) && (player.getLevel() <= MAX_LEVEL))
		{
			showOnScreenMsg(player, NpcStringId.QUEEN_NAVARI_HAS_SENT_A_LETTER_NCLICK_THE_QUESTION_MARK_ICON_TO_READ, ExShowScreenMessage.TOP_CENTER, 10000);
			player.sendPacket(new TutorialShowQuestionMark(getId()));
		}
	}
	
	@Override
	public void onQuestAborted(PlayerInstance player)
	{
		final QuestState st = getQuestState(player, true);
		
		st.startQuest();
		player.sendPacket(SystemMessageId.THIS_QUEST_CANNOT_BE_DELETED);
	}
	
	public static void main(String[] args)
	{
		new Q10782_LettersFromTheQueenForsakenPlains();
	}
}