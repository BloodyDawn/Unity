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
package quests.Q10301_ShadowOfTerrorBlackishRedFog;

import org.l2junity.gameserver.enums.QuestSound;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.ListenerRegisterType;
import org.l2junity.gameserver.model.events.annotations.RegisterEvent;
import org.l2junity.gameserver.model.events.annotations.RegisterType;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerLevelChanged;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerLogin;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerPressTutorialMark;
import org.l2junity.gameserver.model.quest.Quest;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.model.quest.State;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.network.client.send.ExShowScreenMessage;
import org.l2junity.gameserver.network.client.send.TutorialShowHtml;
import org.l2junity.gameserver.network.client.send.TutorialShowQuestionMark;
import org.l2junity.gameserver.network.client.send.string.NpcStringId;

/**
 * Shadow of Terror: Blackish Red Fog (10301)
 * @author St3eT
 */
public final class Q10301_ShadowOfTerrorBlackishRedFog extends Quest
{
	// NPCs
	private static final int LADA = 33100;
	private static final int MISO = 33956;
	private static final int LARGE_VERDANT_WILDS = 33489;
	private static final int WHISP = 27456;
	// Items
	private static final int LADA_LETTER = 17725; // Lada's Letter
	private static final int GLIMMER_CRYSTAL = 17604; // Glimmer Crystal
	private static final int CAPSULED_WHISP = 17588; // Calsuled Whisp
	private static final int FAIRY = 17380; // Agathion - Fairy
	// Skills
	private static final int WHISP_SKILL = 12001;
	// Misc
	private static final int MIN_LEVEL = 88;
	
	public Q10301_ShadowOfTerrorBlackishRedFog()
	{
		super(10301);
		addStartNpc(LADA);
		addTalkId(LADA, MISO);
		addSkillSeeId(LARGE_VERDANT_WILDS);
		addAttackId(WHISP);
		
		addCondMinLevel(MIN_LEVEL, "33100-08.htm");
		registerQuestItems(CAPSULED_WHISP, GLIMMER_CRYSTAL);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		String htmltext = null;
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "33100-02.htm":
			case "33100-03.htm":
			case "33956-02.html":
			{
				htmltext = event;
				break;
			}
			case "33100-04.htm":
			{
				qs.startQuest();
				qs.setCond(2);
				htmltext = event;
				giveItems(player, GLIMMER_CRYSTAL, 10);
				takeItems(player, LADA_LETTER, -1);
				break;
			}
			case "33956-03.html":
			{
				if (qs.isCond(5))
				{
					if (player.getLevel() >= MIN_LEVEL)
					{
						qs.exitQuest(false, true);
						addExpAndSp(player, 26_920_620, 6_460);
						giveItems(player, FAIRY, 1);
						giveAdena(player, 1_863_420, false);
						htmltext = event;
					}
				}
				break;
			}
			case "giveCrystals":
			{
				giveItems(player, GLIMMER_CRYSTAL, 5);
				htmltext = "33100-06.html";
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		
		switch (qs.getState())
		{
			case State.CREATED:
			{
				if (npc.getId() == LADA)
				{
					htmltext = "33100-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if (npc.getId() == LADA)
				{
					if (qs.isCond(2))
					{
						htmltext = "33100-05.html";
					}
					else if (qs.isCond(3))
					{
						htmltext = "33100-07.html";
					}
				}
				else if (npc.getId() == MISO)
				{
					if (qs.isCond(5))
					{
						htmltext = "33956-01.html";
					}
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onSkillSee(Npc npc, PlayerInstance caster, Skill skill, WorldObject[] targets, boolean isSummon)
	{
		final QuestState qs = getQuestState(caster, false);
		if ((qs != null) && qs.isCond(2) && (skill.getId() == WHISP_SKILL))
		{
			final Npc whisp = addSpawn(WHISP, caster, true, 20000);
			whisp.setTitle(caster.getName());
			whisp.broadcastInfo();
		}
		return super.onSkillSee(npc, caster, skill, targets, isSummon);
	}
	
	@Override
	public String onAttack(Npc npc, PlayerInstance attacker, int damage, boolean isSummon)
	{
		final QuestState qs = getQuestState(attacker, false);
		if ((qs != null) && qs.isCond(2))
		{
			if (getRandom(1000) < 500)
			{
				showOnScreenMsg(attacker, NpcStringId.YOU_VE_CAPTURED_A_WISP_SUCCESSFULLY, ExShowScreenMessage.TOP_CENTER, 10000);
				takeItems(attacker, GLIMMER_CRYSTAL, -1);
				qs.setCond(3, true);
			}
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LEVEL_CHANGED)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerLevelChanged(OnPlayerLevelChanged event)
	{
		final PlayerInstance player = event.getActiveChar();
		final QuestState qs = getQuestState(player, false);
		
		if ((qs == null) && (event.getOldLevel() < event.getNewLevel()) && canStartQuest(player))
		{
			player.sendPacket(new TutorialShowQuestionMark(getId()));
			playSound(player, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerLogin(OnPlayerLogin event)
	{
		final PlayerInstance player = event.getActiveChar();
		final QuestState qs = getQuestState(player, false);
		
		if ((qs == null) && canStartQuest(player))
		{
			player.sendPacket(new TutorialShowQuestionMark(getId()));
			playSound(player, QuestSound.ITEMSOUND_QUEST_TUTORIAL);
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_PRESS_TUTORIAL_MARK)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerPressTutorialMark(OnPlayerPressTutorialMark event)
	{
		final PlayerInstance player = event.getActiveChar();
		if ((event.getMarkId() == getId()) && canStartQuest(player))
		{
			final String html = getHtm(player.getHtmlPrefix(), "popup.html");
			player.sendPacket(new TutorialShowHtml(html));
			giveItems(player, LADA_LETTER, 1);
		}
	}
}