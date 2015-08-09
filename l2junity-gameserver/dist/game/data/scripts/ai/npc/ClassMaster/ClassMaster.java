/*
 * Copyright (C) 2004-2015 L2J Unity
 * 
 * This file is part of L2J Unity.
 * 
 * L2J Unity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Unity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ai.npc.ClassMaster;

import java.util.Arrays;
import java.util.StringTokenizer;

import org.l2junity.Config;
import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.data.xml.impl.ClassListData;
import org.l2junity.gameserver.data.xml.impl.ClassMasterData;
import org.l2junity.gameserver.data.xml.impl.ClassMasterData.ClassChangeData;
import org.l2junity.gameserver.datatables.ItemTable;
import org.l2junity.gameserver.enums.HtmlActionScope;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.base.ClassId;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.ListenerRegisterType;
import org.l2junity.gameserver.model.events.annotations.RegisterEvent;
import org.l2junity.gameserver.model.events.annotations.RegisterType;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerBypass;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerLevelChanged;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerLogin;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerPressTutorialMark;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerProfessionChange;
import org.l2junity.gameserver.model.holders.ItemChanceHolder;
import org.l2junity.gameserver.model.holders.ItemHolder;
import org.l2junity.gameserver.network.client.send.TutorialCloseHtml;
import org.l2junity.gameserver.network.client.send.TutorialShowHtml;
import org.l2junity.gameserver.network.client.send.TutorialShowQuestionMark;
import org.l2junity.gameserver.network.client.send.UserInfo;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

import ai.npc.AbstractNpcAI;

/**
 * @author Nik
 */
public class ClassMaster extends AbstractNpcAI
{
	// Npc
	private static final int[] CLASS_MASTER =
	{
		31756, // Mulia
		31757, // Ilia
	};
	
	public ClassMaster()
	{
		super(ClassMaster.class.getSimpleName(), "ai/npc");
		addStartNpc(CLASS_MASTER);
		addTalkId(CLASS_MASTER);
		addFirstTalkId(CLASS_MASTER);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		String htmltext = event;
		int classLevelMenu = 0;
		StringTokenizer st = new StringTokenizer(event);
		event = st.nextToken();
		
		if (event.startsWith("1stClass"))
		{
			classLevelMenu = 1;
		}
		else if (event.startsWith("2ndClass"))
		{
			classLevelMenu = 2;
		}
		else if (event.startsWith("3rdClass"))
		{
			classLevelMenu = 3;
		}
		else if (event.startsWith("awaken"))
		{
			classLevelMenu = 4;
		}
		else if (event.startsWith("change_class") && st.hasMoreTokens())
		{
			int classId = Integer.parseInt(st.nextToken());
			if (st.hasMoreTokens())
			{
				int classDataIndex = Integer.parseInt(st.nextToken());
				if (checkAndChangeClass(player, classId, classDataIndex))
				{
					htmltext = getHtm(player.getHtmlPrefix(), "ok.html");
					htmltext = htmltext.replace("%name%", ClassListData.getInstance().getClass(classId).getClientCode());
				}
			}
			else
			{
				htmltext = getHtm(player.getHtmlPrefix(), "templateOptions.html");
				htmltext = htmltext.replace("%name%", ClassListData.getInstance().getClass(classId).getClientCode());
				htmltext = htmltext.replace("%options%", getClassChangeOptions(player, classId, false));
			}
			
		}
		else if (event.startsWith("become_noble"))
		{
			if (!player.isNoble())
			{
				player.setNoble(true);
				player.sendPacket(new UserInfo(player));
				htmltext = getHtm(player.getHtmlPrefix(), "nobleok.html");
			}
		}
		else if (event.startsWith("learn_skills"))
		{
			player.giveAvailableSkills(Config.AUTO_LEARN_FS_SKILLS, true);
		}
		else if (event.startsWith("increase_clan_level"))
		{
			if (!player.isClanLeader())
			{
				htmltext = getHtm(player.getHtmlPrefix(), "noclanleader.html");
			}
			else if (player.getClan().getLevel() >= 5)
			{
				htmltext = getHtm(player.getHtmlPrefix(), "noclanlevel.html");
			}
			else
			{
				player.getClan().changeLevel(5);
			}
		}
		
		if (classLevelMenu > 0)
		{
			if (!ClassMasterData.getInstance().isEnabled())
			{
				htmltext = getHtm(player.getHtmlPrefix(), "disabled.html");
			}
			else if (!validateClassChange(player, -1, false, false))
			{
				htmltext = "<html><body>You cannot change your occupation.</body></html>";
			}
			else
			{
				final ClassId currentClassId = player.getClassId();
				if (currentClassId.level() >= classLevelMenu)
				{
					htmltext = getHtm(player.getHtmlPrefix(), "nomore.html");
				}
				else
				{
					final int minLevel = getMinLevel(classLevelMenu - 1);
					if ((player.getLevel() >= minLevel) || ClassMasterData.getInstance().isShowEntireTree())
					{
						final StringBuilder menu = new StringBuilder(100);
						
						//@formatter:off
						Arrays.stream(ClassId.values())
						.filter(cid -> validateClassId(player, cid))
						.forEach(cid -> menu.append("<a action=\"bypass -h Quest ClassMaster change_class " + cid.getId() + "\">" + ClassListData.getInstance().getClass(cid).getClientCode() + "</a><br>"));
						//@formatter:on
						
						if (menu.length() > 0)
						{
							htmltext = getHtm(player.getHtmlPrefix(), "template.html").replace("%name%", ClassListData.getInstance().getClass(currentClassId).getClientCode()).replace("%menu%", menu.toString());
						}
						else
						{
							htmltext = getHtm(player.getHtmlPrefix(), "comebacklater.html").replace("%level%", String.valueOf(getMinLevel(classLevelMenu - 1)));
						}
					}
					else
					{
						if (minLevel < Integer.MAX_VALUE)
						{
							htmltext = getHtm(player.getHtmlPrefix(), "comebacklater.html").replace("%level%", String.valueOf(minLevel));
						}
						else
						{
							htmltext = getHtm(player.getHtmlPrefix(), "nomore.html");
						}
					}
				}
			}
		}
		
		return htmltext;
	}
	
	@RegisterEvent(EventType.ON_PLAYER_PRESS_TUTORIAL_MARK)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerPressTutorialMark(OnPlayerPressTutorialMark event)
	{
		final PlayerInstance player = event.getActiveChar();
		
		if ((event.getMarkId() != 1001) || !validateClassChange(player, -1, true, true))
		{
			return;
		}
		
		final ClassId currentClassId = player.getClassId();
		String msg = getHtm(player.getHtmlPrefix(), "tutorialtemplate.html");
		msg = msg.replaceAll("%name%", ClassListData.getInstance().getClass(currentClassId).getEscapedClientCode());
		
		final StringBuilder menu = new StringBuilder(100);
		
		//@formatter:off
		Arrays.stream(ClassId.values())
		.filter(cid -> validateClassId(player, cid))
		.forEach(cid -> menu.append("<a action=\"bypass -h Quest ClassMaster CO " + cid.getId() + "\">" + ClassListData.getInstance().getClass(cid).getEscapedClientCode() + "</a><br>"));
		//@formatter:on
		
		msg = msg.replaceAll("%menu%", menu.toString());
		player.sendPacket(new TutorialShowHtml(msg));
	}
	
	@RegisterEvent(EventType.ON_PLAYER_BYPASS)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerBypass(OnPlayerBypass event)
	{
		final PlayerInstance player = event.getActiveChar();
		
		if (!player.getFloodProtectors().getServerBypass().tryPerformAction("changeclass"))
		{
			return;
		}
		
		// Tutorial Bypass
		StringTokenizer st = new StringTokenizer(event.getCommand());
		if (st.countTokens() < 3)
		{
			return;
		}
		
		st.nextToken(); // Quest
		st.nextToken(); // Class Master
		if (!st.nextToken().startsWith("CO"))
		{
			return;
		}
		
		if (!st.hasMoreTokens())
		{
			player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
			player.clearHtmlActions(HtmlActionScope.TUTORIAL_HTML);
			return;
		}
		
		int selectedClassId = Integer.parseInt(st.nextToken());
		if (validateClassChange(player, selectedClassId, true, true))
		{
			if (st.hasMoreTokens())
			{
				int classDataIndex = Integer.parseInt(st.nextToken());
				checkAndChangeClass(player, selectedClassId, classDataIndex);
				player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
				player.clearHtmlActions(HtmlActionScope.TUTORIAL_HTML);
				return;
			}
			
			String msg = getHtm(player.getHtmlPrefix(), "tutorialtemplateOptions.html");
			msg = msg.replaceAll("%name%", ClassListData.getInstance().getClass(selectedClassId).getEscapedClientCode());
			msg = msg.replaceAll("%options%", getClassChangeOptions(player, selectedClassId, true));
			player.sendPacket(new TutorialShowHtml(msg));
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_PROFESSION_CHANGE)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerProfessionChange(OnPlayerProfessionChange event)
	{
		final PlayerInstance player = event.getActiveChar();
		if (validateClassChange(player, -1, true, true))
		{
			player.sendPacket(new TutorialShowQuestionMark(1001));
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LEVEL_CHANGED)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerLevelChanged(OnPlayerLevelChanged event)
	{
		final PlayerInstance player = event.getActiveChar();
		if (validateClassChange(player, -1, true, true))
		{
			player.sendPacket(new TutorialShowQuestionMark(1001));
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerLogin(OnPlayerLogin event)
	{
		final PlayerInstance player = event.getActiveChar();
		if (validateClassChange(player, -1, true, true))
		{
			player.sendPacket(new TutorialShowQuestionMark(1001));
		}
	}
	
	private static boolean checkAndChangeClass(PlayerInstance player, int classId, int classDataIndex)
	{
		final ClassChangeData data = ClassMasterData.getInstance().getClassChangeData(classDataIndex);
		
		if (data == null)
		{
			return false;
		}
		
		if (!validateClassChange(player, classId, true, false))
		{
			return false;
		}
		
		if (!data.getCategories().stream().anyMatch(ct -> player.isInCategory(ct)))
		{
			return false;
		}
		
		// Weight/Inventory check
		if (!data.getItemsRewarded().isEmpty() && !player.isInventoryUnder90(false))
		{
			player.sendPacket(SystemMessageId.UNABLE_TO_PROCESS_THIS_REQUEST_UNTIL_YOUR_INVENTORY_S_WEIGHT_AND_SLOT_COUNT_ARE_LESS_THAN_80_PERCENT_OF_CAPACITY);
			return false;
		}
		
		// check if player have all required items for class transfer
		for (ItemHolder holder : data.getItemsRequired())
		{
			if (player.getInventory().getInventoryItemCount(holder.getId(), -1) < holder.getCount())
			{
				player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT2);
				return false;
			}
		}
		
		// get all required items for class transfer
		for (ItemChanceHolder holder : data.getItemsRequired())
		{
			// Lucky ones dont get the item removed if the item is with chance to be removed.
			if ((holder.getChance() < 100) && (Rnd.get(100) > holder.getChance()))
			{
				continue;
			}
			
			if (!player.destroyItemByItemId("ClassMaster", holder.getId(), holder.getCount(), player, true))
			{
				return false;
			}
		}
		
		// reward player with items
		for (ItemChanceHolder holder : data.getItemsRewarded())
		{
			if ((holder.getChance() >= 100) || (holder.getChance() > Rnd.get(100)))
			{
				player.addItem("ClassMaster", holder.getId(), holder.getCount(), player, true);
			}
		}
		
		player.setClassId(classId);
		
		if (data.isRewardNoblesse())
		{
			if (!player.isNoble())
			{
				player.sendMessage("You have obtained Noblesse status.");
			}
			
			player.setNoble(true);
		}
		
		if (data.isRewardHero())
		{
			if (!player.isHero())
			{
				player.sendMessage("You have obtained Hero status.");
			}
			
			player.setHero(true);
		}
		
		if (player.isSubClassActive())
		{
			player.getSubClasses().get(player.getClassIndex()).setClassId(player.getActiveClass());
		}
		else
		{
			player.setBaseClass(player.getActiveClass());
		}
		
		player.store(false); // Save player cause if server crashes before this char is saved, he will lose class and the money payed for class change.
		player.broadcastUserInfo();
		
		return true;
	}
	
	private static String getClassChangeOptions(PlayerInstance player, int selectedClassId, boolean tutorialWindow)
	{
		final StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < ClassMasterData.getInstance().getClassChangeData().size(); i++)
		{
			ClassChangeData option = ClassMasterData.getInstance().getClassChangeData(i);
			if ((option == null) || !option.getCategories().stream().anyMatch(ct -> player.isInCategory(ct)))
			{
				continue;
			}
			
			sb.append("<tr><td><img src=L2UI_CT1.ChatBalloon_DF_TopCenter width=276 height=1 /></td></tr>");
			sb.append("<tr><td><table bgcolor=3f3f3f width=100%>");
			if (tutorialWindow)
			{
				sb.append("<tr><td align=center><a action=\"bypass -h Quest ClassMaster CO " + selectedClassId + " " + i + "\">" + option.getName() + ":</a></td></tr>");
			}
			else
			{
				sb.append("<tr><td align=center><a action=\"bypass -h Quest ClassMaster change_class " + selectedClassId + " " + i + "\">" + option.getName() + ":</a></td></tr>");
			}
			sb.append("<tr><td><table width=276>");
			sb.append("<tr><td>Requirements:</td></tr>");
			if (option.getItemsRequired().isEmpty())
			{
				sb.append("<tr><td><font color=LEVEL>Free</font></td></tr>");
			}
			else
			{
				option.getItemsRequired().forEach(ih ->
				{
					if (ih.getChance() >= 100)
					{
						sb.append("<tr><td><font color=\"LEVEL\">" + ih.getCount() + "</font></td><td>" + ItemTable.getInstance().getTemplate(ih.getId()).getName() + "</td><td width=30></td></tr>");
					}
					else
					{
						sb.append("<tr><td><font color=\"LEVEL\">" + ih.getCount() + "</font></td><td>" + ItemTable.getInstance().getTemplate(ih.getId()).getName() + "</td><td width=30><font color=LEVEL>" + ih.getChance() + "%</font></td></tr>");
					}
				});
			}
			sb.append("<tr><td>Rewards:</td></tr>");
			if (option.getItemsRewarded().isEmpty())
			{
				if (option.isRewardNoblesse())
				{
					sb.append("<tr><td><font color=\"LEVEL\">Noblesse status.</font></td></tr>");
				}
				
				if (option.isRewardHero())
				{
					sb.append("<tr><td><font color=\"LEVEL\">Hero status.</font></td></tr>");
				}
				
				if (!option.isRewardNoblesse() && !option.isRewardHero())
				{
					sb.append("<tr><td><font color=LEVEL>none</font></td></tr>");
				}
			}
			else
			{
				option.getItemsRewarded().forEach(ih ->
				{
					if (ih.getChance() >= 100)
					{
						sb.append("<tr><td><font color=\"LEVEL\">" + ih.getCount() + "</font></td><td>" + ItemTable.getInstance().getTemplate(ih.getId()).getName() + "</td><td width=30></td></tr>");
					}
					else
					{
						sb.append("<tr><td><font color=\"LEVEL\">" + ih.getCount() + "</font></td><td>" + ItemTable.getInstance().getTemplate(ih.getId()).getName() + "</td><td width=30><font color=LEVEL>" + ih.getChance() + "%</font></td></tr>");
					}
				});
				
				if (option.isRewardNoblesse())
				{
					sb.append("<tr><td><font color=\"LEVEL\">Noblesse status.</font></td></tr>");
				}
				if (option.isRewardHero())
				{
					sb.append("<tr><td><font color=\"LEVEL\">Hero status.</font></td></tr>");
				}
			}
			sb.append("</table></td></tr>");
			sb.append("</table></td></tr>");
			sb.append("<tr><td><img src=L2UI_CT1.ChatBalloon_DF_TopCenter width=276 height=1 /></td></tr>");
		}
		
		return sb.toString();
	}
	
	/**
	 * @param level - current skillId level (0 - start, 1 - first, etc)
	 * @return minimum player level required for next class transfer
	 */
	private static int getMinLevel(int level)
	{
		switch (level)
		{
			case 0:
				return 20;
			case 1:
				return 40;
			case 2:
				return 76;
			case 3:
				return 85;
			default:
				return Integer.MAX_VALUE;
		}
	}
	
	private static boolean validateClassChange(PlayerInstance player, int newClassId, boolean validateLevel, boolean popup)
	{
		if (!ClassMasterData.getInstance().isEnabled())
		{
			return false;
		}
		
		if (validateLevel && (player.getLevel() < (getMinLevel(player.getClassId().level()))))
		{
			if (!ClassMasterData.getInstance().isShowEntireTree())
			{
				return false;
			}
		}
		
		// Make ClassId check also if classId is present
		if ((newClassId >= 0) && (newClassId < ClassId.values().length))
		{
			ClassId newCID = ClassId.values()[newClassId];
			if (!validateClassId(player, newCID))
			{
				return false;
			}
		}
		
		if (popup)
		{
			if (!ClassMasterData.getInstance().isClassChangeAvailableShowPopup(player))
			{
				return false;
			}
		}
		else
		{
			if (!ClassMasterData.getInstance().isClassChangeAvailable(player))
			{
				return false;
			}
		}
		
		return true;
	}
	
	private static boolean validateClassId(PlayerInstance player, ClassId newCID)
	{
		if ((newCID == null) || (newCID.getRace() == null))
		{
			return false;
		}
		
		if ((newCID == ClassId.INSPECTOR) && (player.getTotalSubClasses() < 2))
		{
			return false;
		}
		
		if (player.getClassId().equals(newCID.getParent()))
		{
			return true;
		}
		
		if (ClassMasterData.getInstance().isShowEntireTree() && newCID.childOf(player.getClassId()))
		{
			return true;
		}
		
		return false;
	}
	
	public static void main(String[] args)
	{
		new ClassMaster();
	}
}