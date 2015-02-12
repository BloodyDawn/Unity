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
package ai.npc.AwakeningMaster;

import org.l2junity.gameserver.ThreadPoolManager;
import org.l2junity.gameserver.data.xml.impl.SkillTreesData;
import org.l2junity.gameserver.enums.CategoryType;
import org.l2junity.gameserver.enums.UserInfoType;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.L2PcInstance;
import org.l2junity.gameserver.model.base.ClassId;
import org.l2junity.gameserver.model.entity.Hero;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.ListenerRegisterType;
import org.l2junity.gameserver.model.events.annotations.RegisterEvent;
import org.l2junity.gameserver.model.events.annotations.RegisterType;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerChangeToAwakenedClass;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.network.SystemMessageId;
import org.l2junity.gameserver.network.serverpackets.AcquireSkillList;
import org.l2junity.gameserver.network.serverpackets.ExChangeToAwakenedClass;
import org.l2junity.gameserver.network.serverpackets.ExShowUsm;
import org.l2junity.gameserver.network.serverpackets.SocialAction;
import org.l2junity.gameserver.network.serverpackets.UserInfo;

import quests.Q10338_SeizeYourDestiny.Q10338_SeizeYourDestiny;
import ai.npc.AbstractNpcAI;

/**
 * AwakeningMaster AI.
 * @author Sdw
 */
public final class AwakeningMaster extends AbstractNpcAI
{
	// NPCs
	private static final int SIGEL_MASTER = 33397;
	private static final int TYRR_MASTER = 33398;
	private static final int OTHELL_MASTER = 33399;
	private static final int YUL_MASTER = 33400;
	private static final int FEOH_MASTER = 33401;
	private static final int ISS_MASTER = 33402;
	private static final int WYNN_MASTER = 33403;
	private static final int AEORE_MASTER = 33404;
	// Items
	private static final int SCROLL_OF_AFTERLIFE = 17600;
	private final static int ABELIUS_POWER = 32264;
	private final static int SAPYROS_POWER = 32265;
	private final static int ASHAGEN_POWER = 32266;
	private final static int CRANIGG_POWER = 32267;
	private final static int SOLTKREIG_POWER = 32268;
	private final static int NAVIAROPE_POWER = 32269;
	private final static int LEISTER_POWER = 32270;
	private final static int LAKCIS_POWER = 32271;
	
	private AwakeningMaster()
	{
		super(AwakeningMaster.class.getSimpleName(), "ai/npc");
		addStartNpc(SIGEL_MASTER, TYRR_MASTER, OTHELL_MASTER, YUL_MASTER, FEOH_MASTER, ISS_MASTER, WYNN_MASTER, AEORE_MASTER);
		addTalkId(SIGEL_MASTER, TYRR_MASTER, OTHELL_MASTER, YUL_MASTER, FEOH_MASTER, ISS_MASTER, WYNN_MASTER, AEORE_MASTER);
		addFirstTalkId(SIGEL_MASTER, TYRR_MASTER, OTHELL_MASTER, YUL_MASTER, FEOH_MASTER, ISS_MASTER, WYNN_MASTER, AEORE_MASTER);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, L2PcInstance player)
	{
		final QuestState st = getQuestState(player, true);
		if (st == null)
		{
			return null;
		}
		String htmltext = null;
		switch (event)
		{
			case "awakening":
			{
				final QuestState st2 = player.getQuestState(Q10338_SeizeYourDestiny.class.getSimpleName());
				if (st.hasQuestItems(SCROLL_OF_AFTERLIFE) && (player.getLevel() > 84) && (!player.isSubClassActive() || player.isDualClassActive()) && player.isInCategory(CategoryType.FOURTH_CLASS_GROUP) && (st2 != null) && st2.isCompleted())
				{
					switch (npc.getId())
					{
						case SIGEL_MASTER:
						{
							if (!player.isInCategory(CategoryType.SIGEL_CANDIDATE))
							{
								return SIGEL_MASTER + "-no_class.htm";
							}
							break;
						}
						case TYRR_MASTER:
						{
							if (!player.isInCategory(CategoryType.TYRR_CANDIDATE))
							{
								return TYRR_MASTER + "-no_class.htm";
							}
							break;
						}
						case OTHELL_MASTER:
						{
							if (!player.isInCategory(CategoryType.OTHELL_CANDIDATE))
							{
								return OTHELL_MASTER + "-no_class.htm";
							}
							break;
						}
						case YUL_MASTER:
						{
							if (!player.isInCategory(CategoryType.YUL_CANDIDATE))
							{
								return YUL_MASTER + "-no_class.htm";
							}
							break;
						}
						case FEOH_MASTER:
						{
							if (!player.isInCategory(CategoryType.FEOH_CANDIDATE))
							{
								return FEOH_MASTER + "-no_class.htm";
							}
							break;
						}
						case ISS_MASTER:
						{
							if (!player.isInCategory(CategoryType.ISS_CANDIDATE))
							{
								return ISS_MASTER + "-no_class.htm";
							}
							break;
						}
						case WYNN_MASTER:
						{
							if (!player.isInCategory(CategoryType.WYNN_CANDIDATE))
							{
								return WYNN_MASTER + "-no_class.htm";
							}
							break;
						}
						case AEORE_MASTER:
						{
							if (!player.isInCategory(CategoryType.AEORE_CANDIDATE))
							{
								return AEORE_MASTER + "-no_class.htm";
							}
							break;
						}
					}
					
					for (ClassId newClass : player.getClassId().getNextClassIds())
					{
						player.sendPacket(new ExChangeToAwakenedClass(newClass.getId()));
					}
				}
				else
				{
					return npc.getId() + "-no.htm";
				}
				break;
			}
		}
		return htmltext;
	}
	
	@RegisterEvent(EventType.ON_PLAYER_CHANGE_TO_AWAKENED_CLASS)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerChangeToAwakenedClass(OnPlayerChangeToAwakenedClass event)
	{
		final L2PcInstance player = event.getActiveChar();
		
		if (player.isSubClassActive() && !player.isDualClassActive())
		{
			return;
		}
		
		if ((player.getLevel() < 85) || !player.isInCategory(CategoryType.FOURTH_CLASS_GROUP))
		{
			return;
		}
		
		final QuestState st = player.getQuestState(Q10338_SeizeYourDestiny.class.getSimpleName());
		
		if ((st == null) || !st.isCompleted())
		{
			return;
		}
		
		if (player.isHero() || Hero.getInstance().isUnclaimedHero(player.getObjectId()))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AWAKEN_WHEN_YOU_ARE_A_HERO_OR_ON_THE_WAIT_LIST_FOR_HERO_STATUS);
			return;
		}
		
		if (player.getInventory().getSize(false) >= (player.getInventoryLimit() * 0.8))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AWAKEN_DUE_TO_YOUR_CURRENT_INVENTORY_WEIGHT_PLEASE_ORGANIZE_YOUR_INVENTORY_AND_TRY_AGAIN_DWARVEN_CHARACTERS_MUST_BE_AT_20_OR_BELOW_THE_INVENTORY_MAX_TO_AWAKEN);
			return;
		}
		
		if (player.isMounted() || player.isTransformed())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AWAKEN_WHILE_YOU_RE_TRANSFORMED_OR_RIDING);
			return;
		}
		
		final ItemInstance item = player.getInventory().getItemByItemId(SCROLL_OF_AFTERLIFE);
		if (item == null)
		{
			return;
		}
		
		if (!player.destroyItem("Awakening", item, player, true))
		{
			return;
		}
		
		for (ClassId newClass : player.getClassId().getNextClassIds())
		{
			player.setClassId(newClass.getId());
			if (player.isDualClassActive())
			{
				player.getSubClasses().get(player.getClassIndex()).setClassId(player.getActiveClass());
			}
			else
			{
				player.setBaseClass(player.getActiveClass());
			}
			player.sendPacket(SystemMessageId.CONGRATULATIONS_YOU_VE_COMPLETED_A_CLASS_TRANSFER);
			final UserInfo ui = new UserInfo(player, false);
			ui.addComponentType(UserInfoType.BASIC_INFO);
			ui.addComponentType(UserInfoType.MAX_HPCPMP);
			player.sendPacket(ui);
			player.broadcastInfo();
			
			int itemId = ABELIUS_POWER; // Sigel
			if (player.isInCategory(CategoryType.TYRR_GROUP))
			{
				itemId = SAPYROS_POWER;
			}
			else if (player.isInCategory(CategoryType.OTHELL_GROUP))
			{
				itemId = ASHAGEN_POWER;
			}
			else if (player.isInCategory(CategoryType.YUL_GROUP))
			{
				itemId = CRANIGG_POWER;
			}
			else if (player.isInCategory(CategoryType.FEOH_GROUP))
			{
				itemId = SOLTKREIG_POWER;
			}
			else if (player.isInCategory(CategoryType.ISS_GROUP))
			{
				itemId = NAVIAROPE_POWER;
			}
			else if (player.isInCategory(CategoryType.WYNN_GROUP))
			{
				itemId = LEISTER_POWER;
			}
			else if (player.isInCategory(CategoryType.AEORE_GROUP))
			{
				itemId = LAKCIS_POWER;
			}
			player.broadcastPacket(new SocialAction(player.getObjectId(), 20));
			giveItems(player, itemId, 1);
			
			SkillTreesData.getInstance().cleanSkillUponAwakening(player);
			player.sendPacket(new AcquireSkillList(player));
			player.sendSkillList();
		}
		
		ThreadPoolManager.getInstance().scheduleGeneral(() ->
		{
			player.sendPacket(ExShowUsm.AWAKENING_END);
		}, 10000);
	}
	
	public static void main(String[] args)
	{
		new AwakeningMaster();
	}
}
