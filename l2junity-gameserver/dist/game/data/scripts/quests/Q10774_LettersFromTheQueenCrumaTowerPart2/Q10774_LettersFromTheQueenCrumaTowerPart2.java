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
package quests.Q10774_LettersFromTheQueenCrumaTowerPart2;

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
 * Letters from the Queen: Cruma Tower, Part 2 (10774)
 * @author malyelfik
 */
public final class Q10774_LettersFromTheQueenCrumaTowerPart2 extends Quest
{
	// NPCs
	private static final int SYLVAIN = 30070;
	private static final int ROMBEL = 30487;
	// Items
	private static final int SOE_DION_TOWN = 39595;
	private static final int SOE_CRUMA_TOWER = 39596;
	private static final int ENCHANT_ARMOR_C = 952;
	private static final int STEEL_DOOR_GUILD_COIN = 37045;
	// Location
	private static final Location TELEPORT_LOC = new Location(16014, 142326, -2688);
	// Misc
	private static final int MIN_LEVEL = 46;
	private static final int MAX_LEVEL = 50;
	
	public Q10774_LettersFromTheQueenCrumaTowerPart2()
	{
		super(10774, Q10774_LettersFromTheQueenCrumaTowerPart2.class.getSimpleName(), "Letters from the Queen: Cruma Tower, Part 2");
		addTalkId(SYLVAIN, ROMBEL);
		
		addCondRace(Race.ERTHEIA, "");
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "");
		registerQuestItems(SOE_DION_TOWN, SOE_CRUMA_TOWER);
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
			case "30070-02.html":
			case "30487-02.html":
				break;
			case "30070-03.html":
			{
				if (qs.isCond(1))
				{
					qs.setCond(2, true);
					giveItems(player, SOE_CRUMA_TOWER, 1);
					showOnScreenMsg(player, NpcStringId.TRY_USING_THE_TELEPORT_SCROLL_SYLVAIN_GAVE_YOU_TO_GO_TO_CRUMA_TOWER2, ExShowScreenMessage.TOP_CENTER, 8000);
				}
				break;
			}
			case "30487-03.html":
			{
				if (qs.isCond(2))
				{
					giveItems(player, ENCHANT_ARMOR_C, 2);
					giveItems(player, STEEL_DOOR_GUILD_COIN, 11);
					addExpAndSp(player, 483840, 116);
					showOnScreenMsg(player, NpcStringId.GROW_STRONGER_HERE_UNTIL_YOU_RECEIVE_THE_NEXT_LETTER_FROM_QUEEN_NAVARI_AT_LV_52, ExShowScreenMessage.TOP_CENTER, 8000);
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
			if (npc.getId() == SYLVAIN)
			{
				htmltext = (qs.isCond(1)) ? "30070-01.html" : "30070-04.html";
			}
			else if (qs.isCond(2))
			{
				htmltext = "30487-01.html";
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
			player.sendPacket(new PlaySound(3, "Npcdialog1.serenia_quest_4", 0, 0, 0, 0, 0));
			player.sendPacket(new TutorialShowHtml(getHtm(player.getHtmlPrefix(), "popup.html")));
			giveItems(player, SOE_DION_TOWN, 1);
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_BYPASS)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerBypass(OnPlayerBypass event)
	{
		final String command = event.getCommand();
		final PlayerInstance player = event.getActiveChar();
		final QuestState st = getQuestState(player, false);
		
		if (command.equals("Q10774_teleport") && (st != null) && st.isCond(1) && hasQuestItems(player, SOE_DION_TOWN))
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
				takeItems(player, SOE_DION_TOWN, -1);
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
		new Q10774_LettersFromTheQueenCrumaTowerPart2();
	}
}
