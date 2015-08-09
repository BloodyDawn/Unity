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
package org.l2junity.gameserver.model.actor.instance;

import java.util.Arrays;
import java.util.StringTokenizer;

import org.l2junity.Config;
import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.cache.HtmCache;
import org.l2junity.gameserver.data.xml.impl.ClassListData;
import org.l2junity.gameserver.data.xml.impl.ClassMasterData;
import org.l2junity.gameserver.data.xml.impl.ClassMasterData.ClassChangeData;
import org.l2junity.gameserver.datatables.ItemTable;
import org.l2junity.gameserver.enums.HtmlActionScope;
import org.l2junity.gameserver.enums.InstanceType;
import org.l2junity.gameserver.model.actor.templates.L2NpcTemplate;
import org.l2junity.gameserver.model.base.ClassId;
import org.l2junity.gameserver.model.holders.ItemChanceHolder;
import org.l2junity.gameserver.model.holders.ItemHolder;
import org.l2junity.gameserver.network.client.send.NpcHtmlMessage;
import org.l2junity.gameserver.network.client.send.TutorialCloseHtml;
import org.l2junity.gameserver.network.client.send.TutorialShowHtml;
import org.l2junity.gameserver.network.client.send.TutorialShowQuestionMark;
import org.l2junity.gameserver.network.client.send.UserInfo;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * This class ...
 * @version $Revision: 1.4.2.1.2.7 $ $Date: 2005/03/27 15:29:32 $
 */
public final class L2ClassMasterInstance extends L2MerchantInstance
{
	public L2ClassMasterInstance(L2NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2ClassMasterInstance);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		return "data/html/classmaster/" + pom + ".htm";
	}
	
	@Override
	public void onBypassFeedback(PlayerInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command);
		command = st.nextToken();
		if (command.startsWith("1stClass"))
		{
			showHtmlMenu(player, getObjectId(), 1);
		}
		else if (command.startsWith("2ndClass"))
		{
			showHtmlMenu(player, getObjectId(), 2);
		}
		else if (command.startsWith("3rdClass"))
		{
			showHtmlMenu(player, getObjectId(), 3);
		}
		else if (command.startsWith("awaken"))
		{
			showHtmlMenu(player, getObjectId(), 4);
		}
		else if (command.startsWith("change_class") && st.hasMoreTokens())
		{
			int classId = Integer.parseInt(st.nextToken());
			if (st.hasMoreTokens())
			{
				int classDataIndex = Integer.parseInt(st.nextToken());
				if (checkAndChangeClass(player, classId, classDataIndex))
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getHtmlPrefix(), "data/html/classmaster/ok.htm");
					html.replace("%name%", ClassListData.getInstance().getClass(classId).getClientCode());
					player.sendPacket(html);
				}
			}
			else
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getHtmlPrefix(), "data/html/classmaster/templateOptions.htm");
				html.replace("%name%", ClassListData.getInstance().getClass(classId).getClientCode());
				html.replace("%options%", getClassChangeOptions(player, classId, false));
				html.replace("%objectId%", getObjectId());
				player.sendPacket(html);
			}
			
		}
		else if (command.startsWith("become_noble"))
		{
			if (!player.isNoble())
			{
				player.setNoble(true);
				player.sendPacket(new UserInfo(player));
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getHtmlPrefix(), "data/html/classmaster/nobleok.htm");
				player.sendPacket(html);
			}
		}
		else if (command.startsWith("learn_skills"))
		{
			player.giveAvailableSkills(Config.AUTO_LEARN_FS_SKILLS, true);
		}
		else if (command.startsWith("increase_clan_level"))
		{
			if (!player.isClanLeader())
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getHtmlPrefix(), "data/html/classmaster/noclanleader.htm");
				player.sendPacket(html);
			}
			else if (player.getClan().getLevel() >= 5)
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getHtmlPrefix(), "data/html/classmaster/noclanlevel.htm");
				player.sendPacket(html);
			}
			else
			{
				player.getClan().changeLevel(5);
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	public static void onTutorialLink(PlayerInstance player, String request)
	{
		if (request == null)
		{
			return;
		}
		
		if (!player.getFloodProtectors().getServerBypass().tryPerformAction("changeclass"))
		{
			return;
		}
		
		StringTokenizer st = new StringTokenizer(request, "_");
		
		if (!st.nextToken().startsWith("CO"))
		{
			return;
		}
		
		boolean closeWnd = true;
		if (st.hasMoreTokens())
		{
			try
			{
				int classId = Integer.parseInt(st.nextToken());
				if (st.hasMoreTokens())
				{
					int classDataIndex = Integer.parseInt(st.nextToken());
					if (checkAndChangeClass(player, classId, classDataIndex))
					{
						// Re-show question mark if player can change class again.
						if ((player.getLevel() >= getMinLevel(player.getClassId().level())) && ClassMasterData.getInstance().isClassChangeAvailableShowPopup(player))
						{
							showQuestionMark(player);
						}
					}
				}
				else
				{
					closeWnd = false;
					showTutorialHtml(player, classId);
				}
			}
			catch (NumberFormatException e)
			{
			}
		}
		else
		{
		
		}
		
		if (closeWnd)
		{
			player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
			player.clearHtmlActions(HtmlActionScope.TUTORIAL_HTML);
		}
	}
	
	public static void onTutorialQuestionMark(PlayerInstance player, int number)
	{
		if (number != 1001)
		{
			return;
		}
		
		showTutorialHtml(player, -1);
	}
	
	public static void showQuestionMark(PlayerInstance player)
	{
		if (!ClassMasterData.getInstance().isClassChangeAvailableShowPopup(player))
		{
			return;
		}
		
		player.sendPacket(new TutorialShowQuestionMark(1001));
	}
	
	private static void showHtmlMenu(PlayerInstance player, int objectId, int level)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		
		if (!ClassMasterData.getInstance().isEnabled())
		{
			html.setFile(player.getHtmlPrefix(), "data/html/classmaster/disabled.htm");
		}
		else if (!ClassMasterData.getInstance().isClassChangeAvailable(player))
		{
			html.setHtml("<html><body>You cannot change your occupation.</body></html>");
		}
		else
		{
			final ClassId currentClassId = player.getClassId();
			if (currentClassId.level() >= level)
			{
				html.setFile(player.getHtmlPrefix(), "data/html/classmaster/nomore.htm");
			}
			else
			{
				final int minLevel = getMinLevel(currentClassId.level());
				if ((player.getLevel() >= minLevel) || ClassMasterData.getInstance().isShowEntireTree())
				{
					final StringBuilder menu = new StringBuilder(100);
					for (ClassId cid : ClassId.values())
					{
						if ((cid == ClassId.INSPECTOR) && (player.getTotalSubClasses() < 2))
						{
							continue;
						}
						if (validateClassId(currentClassId, cid) && (cid.level() == level))
						{
							menu.append("<a action=\"bypass -h npc_%objectId%_change_class " + cid.getId() + "\">" + ClassListData.getInstance().getClass(cid).getClientCode() + "</a><br>");
						}
					}
					
					if (menu.length() > 0)
					{
						html.setFile(player.getHtmlPrefix(), "data/html/classmaster/template.htm");
						html.replace("%name%", ClassListData.getInstance().getClass(currentClassId).getClientCode());
						html.replace("%menu%", menu.toString());
					}
					else
					{
						html.setFile(player.getHtmlPrefix(), "data/html/classmaster/comebacklater.htm");
						html.replace("%level%", String.valueOf(getMinLevel(level - 1)));
					}
				}
				else
				{
					if (minLevel < Integer.MAX_VALUE)
					{
						html.setFile(player.getHtmlPrefix(), "data/html/classmaster/comebacklater.htm");
						html.replace("%level%", String.valueOf(minLevel));
					}
					else
					{
						html.setFile(player.getHtmlPrefix(), "data/html/classmaster/nomore.htm");
					}
				}
			}
		}
		
		html.replace("%objectId%", String.valueOf(objectId));
		// html.replace("%req_items%", getRequiredItems(level));
		player.sendPacket(html);
	}
	
	private static void showTutorialHtml(PlayerInstance player, int preselectedClassId)
	{
		final ClassId currentClassId = player.getClassId();
		if ((getMinLevel(currentClassId.level()) > player.getLevel()) && !ClassMasterData.getInstance().isShowEntireTree())
		{
			return;
		}
		
		if (Arrays.stream(ClassId.values()).anyMatch(cid -> cid.getId() == preselectedClassId))
		{
			String msg = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), "data/html/classmaster/tutorialtemplateOptions.htm");
			msg = msg.replaceAll("%name%", ClassListData.getInstance().getClass(currentClassId).getEscapedClientCode());
			msg = msg.replaceAll("%options%", getClassChangeOptions(player, preselectedClassId, true));
			player.sendPacket(new TutorialShowHtml(msg));
		}
		else
		{
			String msg = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), "data/html/classmaster/tutorialtemplate.htm");
			msg = msg.replaceAll("%name%", ClassListData.getInstance().getClass(currentClassId).getEscapedClientCode());
			
			final StringBuilder menu = new StringBuilder(100);
			for (ClassId cid : ClassId.values())
			{
				if ((cid == ClassId.INSPECTOR) && (player.getTotalSubClasses() < 2))
				{
					continue;
				}
				if (validateClassId(currentClassId, cid))
				{
					menu.append("<a action=\"link CO_" + cid.getId() + "\">" + ClassListData.getInstance().getClass(cid).getEscapedClientCode() + "</a><br>");
				}
			}
			
			msg = msg.replaceAll("%menu%", menu.toString());
			// msg = msg.replace("%req_items%", getRequiredItems(currentClassId.level() + 1));
			player.sendPacket(new TutorialShowHtml(msg));
		}
		
	}
	
	private static boolean checkAndChangeClass(PlayerInstance player, int classId, int classDataIndex)
	{
		final ClassId currentClassId = player.getClassId();
		final ClassChangeData data = ClassMasterData.getInstance().getClassChangeData(classDataIndex);
		
		if (data == null)
		{
			return false;
		}
		
		if ((getMinLevel(currentClassId.level()) > player.getLevel()) && !ClassMasterData.getInstance().isShowEntireTree())
		{
			return false;
		}
		
		if (!data.getCategories().stream().anyMatch(ct -> player.isInCategory(ct)))
		{
			return false;
		}
		
		if (!validateClassId(currentClassId, classId))
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
				sb.append("<tr><td align=center><a action=\"link CO_" + selectedClassId + "_" + i + "\">" + option.getName() + ":</a></td></tr>");
			}
			else
			{
				sb.append("<tr><td align=center><a action=\"bypass -h npc_%objectId%_change_class " + selectedClassId + " " + i + "\">" + option.getName() + ":</a></td></tr>");
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
	
	/**
	 * Returns true if class change is possible
	 * @param oldCID current player ClassId
	 * @param val new class index
	 * @return
	 */
	private static boolean validateClassId(ClassId oldCID, int val)
	{
		return validateClassId(oldCID, ClassId.getClassId(val));
	}
	
	/**
	 * Returns true if class change is possible
	 * @param oldCID current player ClassId
	 * @param newCID new ClassId
	 * @return true if class change is possible
	 */
	private static boolean validateClassId(ClassId oldCID, ClassId newCID)
	{
		if ((newCID == null) || (newCID.getRace() == null))
		{
			return false;
		}
		
		if (oldCID.equals(newCID.getParent()))
		{
			return true;
		}
		
		if (ClassMasterData.getInstance().isShowEntireTree() && newCID.childOf(oldCID))
		{
			return true;
		}
		
		return false;
	}
}
