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
package quests.Q10404_KekropusLetterAHiddenMeaning;

import org.l2junity.gameserver.enums.HtmlActionScope;
import org.l2junity.gameserver.enums.Race;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.actor.Creature;
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
import org.l2junity.gameserver.model.quest.State;
import org.l2junity.gameserver.network.client.send.ExShowScreenMessage;
import org.l2junity.gameserver.network.client.send.PlaySound;
import org.l2junity.gameserver.network.client.send.TutorialCloseHtml;
import org.l2junity.gameserver.network.client.send.TutorialShowHtml;
import org.l2junity.gameserver.network.client.send.TutorialShowQuestionMark;
import org.l2junity.gameserver.network.client.send.string.NpcStringId;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * Kekropus' Letter: A Hidden Meaning (10404)
 * @author St3eT
 */
public final class Q10404_KekropusLetterAHiddenMeaning extends Quest
{
	// NPCs
	private static final int PATERSON = 33864;
	private static final int SHUVANN = 33867;
	private static final int INVISIBLE_NPC = 19543;
	// Items
	private static final int SOE_TOWN_OF_ADEN = 37116; // Scroll of Escape: Town of Aden
	private static final int SOE_FIELDS_OF_MASSACRE = 37029; // Scroll of Escape: Fields of Massacre
	private static final int EWA = 729; // Scroll: Enchant Weapon (A-grade)
	private static final int STEEL_COIN = 37045; // Steel Door Guild Coin
	// Location
	private static final Location TELEPORT_LOC = new Location(147619, 24681, -1984);
	// Misc
	private static final int MIN_LEVEL = 61;
	private static final int MAX_LEVEL = 64;
	
	public Q10404_KekropusLetterAHiddenMeaning()
	{
		super(10404, Q10404_KekropusLetterAHiddenMeaning.class.getSimpleName(), "Kekropus' Letter: A Hidden Meaning");
		addTalkId(PATERSON, SHUVANN);
		addSeeCreatureId(INVISIBLE_NPC);
		registerQuestItems(SOE_TOWN_OF_ADEN, SOE_FIELDS_OF_MASSACRE);
		addCondNotRace(Race.ERTHEIA, "");
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "");
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
			case "33864-02.html":
			{
				htmltext = event;
				break;
			}
			case "33864-03.html":
			{
				if (st.isCond(1))
				{
					st.setCond(2, true);
					giveItems(player, SOE_FIELDS_OF_MASSACRE, 1);
					htmltext = event;
				}
				break;
			}
			case "33867-02.html":
			{
				if (st.isCond(2))
				{
					st.exitQuest(false, true);
					giveItems(player, EWA, 1);
					giveItems(player, STEEL_COIN, 71);
					if (player.getLevel() >= MIN_LEVEL)
					{
						addExpAndSp(player, 807_240, 193);
					}
					htmltext = event;
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
		
		if (st.getState() == State.STARTED)
		{
			if ((npc.getId() == PATERSON) && st.isCond(1))
			{
				htmltext = "33864-01.html";
			}
			else if (st.isCond(2))
			{
				htmltext = npc.getId() == PATERSON ? "33864-04.html" : "33867-01.html";
			}
		}
		return htmltext;
	}
	
	@Override
	public String onSeeCreature(Npc npc, Creature creature, boolean isSummon)
	{
		if (creature.isPlayer())
		{
			final PlayerInstance player = creature.getActingPlayer();
			final QuestState st = getQuestState(player, false);
			
			if ((st != null) && st.isCond(2))
			{
				showOnScreenMsg(player, NpcStringId.FIELDS_OF_MASSACRE_IS_A_GOOD_HUNTING_ZONE_FOR_LV_61_OR_ABOVE, ExShowScreenMessage.TOP_CENTER, 6000);
			}
		}
		return super.onSeeCreature(npc, creature, isSummon);
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LEVEL_CHANGED)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerLevelChanged(OnPlayerLevelChanged event)
	{
		final PlayerInstance player = event.getActiveChar();
		final QuestState st = getQuestState(player, false);
		final int oldLevel = event.getOldLevel();
		final int newLevel = event.getNewLevel();
		
		if ((st == null) && (oldLevel < newLevel) && (newLevel == MIN_LEVEL) && (player.getRace() != Race.ERTHEIA))
		{
			showOnScreenMsg(player, NpcStringId.KEKROPUS_LETTER_HAS_ARRIVED_NCLICK_THE_QUESTION_MARK_ICON_TO_READ3, ExShowScreenMessage.TOP_CENTER, 6000);
			player.sendPacket(new TutorialShowQuestionMark(getId()));
		}
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
			player.sendPacket(new PlaySound(3, "Npcdialog1.kekrops_quest_5", 0, 0, 0, 0, 0));
			giveItems(player, SOE_TOWN_OF_ADEN, 1);
			player.sendPacket(new TutorialShowHtml(getHtm(player.getHtmlPrefix(), "popup.html")));
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_BYPASS)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerBypass(OnPlayerBypass event)
	{
		final String command = event.getCommand();
		final PlayerInstance player = event.getActiveChar();
		final QuestState st = getQuestState(player, false);
		
		if (command.equals("Q10404_teleport") && (st != null) && st.isCond(1) && hasQuestItems(player, SOE_TOWN_OF_ADEN))
		{
			if (!player.isInCombat())
			{
				player.teleToLocation(TELEPORT_LOC);
				takeItems(player, SOE_TOWN_OF_ADEN, -1);
			}
			else
			{
				showOnScreenMsg(player, NpcStringId.YOU_CANNOT_TELEPORT_IN_COMBAT, ExShowScreenMessage.TOP_CENTER, 6000);
			}
			player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
			player.clearHtmlActions(HtmlActionScope.TUTORIAL_HTML);
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerLogin(OnPlayerLogin event)
	{
		final PlayerInstance player = event.getActiveChar();
		final QuestState st = getQuestState(player, false);
		
		if ((player.getLevel() >= MIN_LEVEL) && (player.getLevel() <= MAX_LEVEL) && (st == null) && (player.getRace() != Race.ERTHEIA))
		{
			showOnScreenMsg(player, NpcStringId.KEKROPUS_LETTER_HAS_ARRIVED_NCLICK_THE_QUESTION_MARK_ICON_TO_READ3, ExShowScreenMessage.TOP_CENTER, 6000);
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
}