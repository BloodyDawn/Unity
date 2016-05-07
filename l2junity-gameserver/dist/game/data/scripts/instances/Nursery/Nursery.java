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
package instances.Nursery;

import org.l2junity.gameserver.enums.CategoryType;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.model.instancezone.Instance;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.network.client.send.ExSendUIEvent;
import org.l2junity.gameserver.network.client.send.ExShowScreenMessage;
import org.l2junity.gameserver.network.client.send.string.NpcStringId;

import instances.AbstractInstance;

/**
 * Nursery instance zone.
 * @author St3eT
 */
public final class Nursery extends AbstractInstance
{
	// NPCs
	private static final int TIE = 33152;
	private static final int[] MONSTERS =
	{
		23033, // Failed Creation
		23034, // Failed Creation
		23035, // Failed Creation
		23036, // Failed Creation
		23037, // Failed Creation
	};
	// Items
	private static final int SCORE_ITEM = 17610; // Tissue Energy Residue
	private static final int REWARD_ITEM = 17602; // Tissue Energy Crystal
	// Skill
	private static final SkillHolder ENERGY_SKILL = new SkillHolder(14228, 1);
	// Misc
	private static final int TEMPLATE_ID = 171;
	
	public Nursery()
	{
		addStartNpc(TIE);
		addFirstTalkId(TIE);
		addTalkId(TIE);
		addKillId(MONSTERS);
	}
	
	@Override
	public void onTimerEvent(String event, StatsSet params, Npc npc, PlayerInstance player)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isNurseryInstance(instance))
		{
			final StatsSet npcVars = npc.getVariables();
			final int gameStage = npcVars.getInt("GAME_STAGE", 0);
			
			switch (event)
			{
				case "CLOCK_TIMER":
				{
					final int gameTime = npcVars.increaseInt("GAME_TIME", 1500, -1);
					instance.getPlayers().forEach(temp -> temp.sendPacket(new ExSendUIEvent(temp, 3, gameTime, npcVars.getInt("GAME_POINTS", 0), 0, 2042, 0, NpcStringId.ELAPSED_TIME.getId())));
					if (gameStage == 1)
					{
						if (gameTime == 0)
						{
							player = instance.getFirstPlayer();
							if ((player != null) && hasQuestItems(player, SCORE_ITEM))
							{
								final int itemCount = (int) getQuestItemsCount(player, SCORE_ITEM);
								takeItems(player, SCORE_ITEM, itemCount);
								npcVars.increaseInt("GAME_POINTS", 0, itemCount);
							}
							instance.despawnGroup("GAME_MONSTERS");
							npcVars.set("GAME_STAGE", 2);
						}
						else
						{
							getTimers().addTimer("CLOCK_TIMER", 1000, npc, null);
						}
					}
					break;
				}
			}
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, PlayerInstance player)
	{
		final Instance instance = npc.getInstanceWorld();
		String htmltext = null;
		
		if (isNurseryInstance(instance))
		{
			final StatsSet npcVars = npc.getVariables();
			
			final int gameStage = npcVars.getInt("GAME_STAGE", 0);
			switch (gameStage)
			{
				case 0:
					htmltext = "GameManager-01.html";
					break;
				case 2:
					htmltext = "GameManager-02.html";
					break;
				case 3:
					htmltext = "GameManager-03.html";
					break;
			}
			
			final BuffInfo energyInfo = player.getEffectList().getBuffInfoByAbnormalType(ENERGY_SKILL.getSkill().getAbnormalType());
			final int energyLv = energyInfo == null ? 0 : energyInfo.getSkill().getAbnormalLvl();
			
			if ((energyLv > 0) && (gameStage == 1) && (energyInfo != null))
			{
				int addPoints = 0;
				if (energyLv == 10)
				{
					addPoints = 40;
				}
				else if (energyLv == 11)
				{
					addPoints = 60;
				}
				else if (energyLv == 12)
				{
					addPoints = 80;
				}
				
				npcVars.set("GAME_POINTS", npcVars.getInt("GAME_POINTS", 0) + addPoints);
				showOnScreenMsg(instance, NpcStringId.SOLDIER_TIE_ABSORBED_REPRODUCTIVE_ENERGY_FROM_YOUR_BODY_AND_CONVERTED_S1_PIECES_OF_BIO_ENERGY, ExShowScreenMessage.TOP_CENTER, 3000, String.valueOf(addPoints));
				energyInfo.stopAllEffects(true);
			}
		}
		return htmltext;
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		if (event.equals("enterInstance"))
		{
			enterInstance(player, npc, TEMPLATE_ID);
		}
		else
		{
			final Instance instance = npc.getInstanceWorld();
			if (isNurseryInstance(instance))
			{
				final StatsSet npcVars = npc.getVariables();
				final int gameStage = npcVars.getInt("GAME_STAGE", 0);
				
				switch (event)
				{
					case "startGame":
					{
						if (gameStage == 0)
						{
							instance.setReenterTime();
							instance.spawnGroup("GAME_MONSTERS");
							getTimers().addTimer("CLOCK_TIMER", 1000, npc, null);
							npcVars.set("GAME_STAGE", 1);
						}
						break;
					}
					case "calculatePoints":
					{
						if (gameStage == 2)
						{
							final int gamePoints = npcVars.getInt("GAME_POINTS", 0);
							int itemCount = 0;
							if ((gamePoints != 0) && (gamePoints <= 800))
							{
								itemCount = 10;
							}
							else if ((gamePoints > 800) && (gamePoints <= 1600))
							{
								itemCount = 60;
							}
							else if ((gamePoints > 1600) && (gamePoints <= 2000))
							{
								itemCount = 160;
							}
							else if ((gamePoints > 2000) && (gamePoints <= 2400))
							{
								itemCount = 200;
							}
							else if ((gamePoints > 2400) && (gamePoints <= 2800))
							{
								itemCount = 240;
							}
							else if ((gamePoints > 2800) && (gamePoints <= 3200))
							{
								itemCount = 280;
							}
							else if ((gamePoints > 3200) && (gamePoints <= 3600))
							{
								itemCount = 320;
							}
							else if ((gamePoints > 3600) && (gamePoints <= 4000))
							{
								itemCount = 360;
							}
							else if (gamePoints > 4000)
							{
								itemCount = 400;
							}
							
							if (gamePoints != 0)
							{
								giveItems(player, REWARD_ITEM, itemCount);
								addExpAndSp(player, 40000 * gamePoints, 0);
							}
							
							npcVars.set("GAME_STAGE", 3);
							instance.finishInstance(0);
						}
						break;
					}
				}
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isNurseryInstance(instance))
		{
			// TODO: maguen chance
			
			// TODO: energy chance
			
			if ((getRandom(10) + 1) < 10)
			{
				int pointsCount = getRandom(6) + 3;
				
				if (killer.isInCategory(CategoryType.SIGEL_GROUP) || killer.isInCategory(CategoryType.AEORE_GROUP))
				{
					pointsCount += 6;
				}
				else if (killer.isInCategory(CategoryType.TYRR_GROUP))
				{
					pointsCount -= 1;
				}
				else if (killer.isInCategory(CategoryType.OTHELL_GROUP))
				{
					pointsCount += 2;
				}
				else if (killer.isInCategory(CategoryType.YUL_GROUP))
				{
					pointsCount += 1;
				}
				else if (killer.isInCategory(CategoryType.FEOH_GROUP) || killer.isInCategory(CategoryType.ISS_GROUP))
				{
					pointsCount += 0;
				}
				else if (killer.isInCategory(CategoryType.WYNN_GROUP))
				{
					pointsCount += 3;
				}
				
				final Npc gameManager = instance.getNpc(TIE);
				if (gameManager != null)
				{
					gameManager.getVariables().increaseInt("GAME_POINTS", pointsCount);
				}
			}
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	private boolean isNurseryInstance(Instance instance)
	{
		return ((instance != null) && (instance.getTemplateId() == TEMPLATE_ID));
	}
	
	public static void main(String[] args)
	{
		new Nursery();
	}
}