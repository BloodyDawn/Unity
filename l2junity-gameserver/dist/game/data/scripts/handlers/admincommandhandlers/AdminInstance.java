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
package handlers.admincommandhandlers;

import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.l2junity.gameserver.cache.HtmCache;
import org.l2junity.gameserver.handler.IAdminCommandHandler;
import org.l2junity.gameserver.instancemanager.InstanceManager;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.html.PageBuilder;
import org.l2junity.gameserver.model.html.PageResult;
import org.l2junity.gameserver.model.html.formatters.BypassParserFormatter;
import org.l2junity.gameserver.model.html.pagehandlers.NextPrevPageHandler;
import org.l2junity.gameserver.model.html.styles.ButtonsStyle;
import org.l2junity.gameserver.model.instancezone.InstanceTemplate;
import org.l2junity.gameserver.network.client.send.NpcHtmlMessage;
import org.l2junity.gameserver.util.BypassParser;

/**
 * Instance admin commands.
 * @author St3eT
 */
public final class AdminInstance implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_instance",
		"admin_instances",
		"admin_instancelist",
	};
	
	@Override
	public boolean useAdminCommand(String command, PlayerInstance activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		
		switch (actualCommand.toLowerCase())
		{
			case "admin_instance":
			case "admin_instances":
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
				html.setFile(activeChar.getHtmlPrefix(), "data/html/admin/instances.htm");
				html.replace("%instCount%", InstanceManager.getInstance().getInstances().size());
				html.replace("%tempCount%", InstanceManager.getInstance().getInstanceTemplates().size());
				activeChar.sendPacket(html);
				break;
			}
			case "admin_instancelist":
			{
				processBypass(activeChar, new BypassParser(command));
				break;
			}
		}
		return true;
	}
	
	public void showTemplateDetails(PlayerInstance activeChar, int instanceId) // TODO: public
	{
		if (InstanceManager.getInstance().getInstanceTemplate(instanceId) != null)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
			html.setHtml(HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), "data/html/admin/instances_detail.htm"));
			activeChar.sendPacket(html);
		}
		else
		{
			activeChar.sendMessage("Instance template with id " + instanceId + " does not exist!");
			useAdminCommand("admin_instance", activeChar);
		}
	}
	
	private void sendTemplateList(PlayerInstance player, int page, BypassParser parser)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player.getHtmlPrefix(), "data/html/admin/instances_list.htm");
		
		final InstanceManager instManager = InstanceManager.getInstance();
		final List<InstanceTemplate> templateList = instManager.getInstanceTemplates().stream().sorted(Comparator.comparingLong(InstanceTemplate::getWorldCount)).collect(Collectors.toList());
		
		//@formatter:off
		final PageResult result = PageBuilder.newBuilder(templateList, 4, "bypass -h admin_instancelist")
			.currentPage(page)
			.pageHandler(NextPrevPageHandler.INSTANCE)
			.formatter(BypassParserFormatter.INSTANCE)
			.style(ButtonsStyle.INSTANCE)
			.bodyHandler((pages, template, sb) ->
		{
			sb.append("<table border=0 cellpadding=0 cellspacing=0 bgcolor=\"363636\">");
			sb.append("<tr><td align=center fixwidth=\"250\"><font color=\"LEVEL\">" + template.getName() + " (" + template.getId() + ")</font></td></tr>");
			sb.append("</table>");

			sb.append("<table border=0 cellpadding=0 cellspacing=0 bgcolor=\"363636\">");
			sb.append("<tr>");
			sb.append("<td align=center fixwidth=\"83\">Active worlds:</td>");
			sb.append("<td align=center fixwidth=\"83\"></td>");
			sb.append("<td align=center fixwidth=\"83\">" + template.getWorldCount() + " / " + (template.getMaxWorlds() == -1 ? "Unlimited" : template.getMaxWorlds()) + "</td>");
			sb.append("</tr>");
			
			sb.append("<tr>");
			sb.append("<td align=center fixwidth=\"83\">Detailed info:</td>");
			sb.append("<td align=center fixwidth=\"83\"></td>");
			sb.append("<td align=center fixwidth=\"83\"><button value=\"Show me!\" action=\"bypass -h admin_instancelist id=" + template.getId() + "\" width=\"85\" height=\"20\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			sb.append("</tr>");
			
			
			sb.append("</table>");
			sb.append("<br>");
		}).build();
		//@formatter:on
		
		html.replace("%pages%", result.getPages() > 0 ? "<center><table width=\"100%\" cellspacing=0><tr>" + result.getPagerTemplate() + "</tr></table></center>" : "");
		html.replace("%data%", result.getBodyTemplate().toString());
		player.sendPacket(html);
	}
	
	private void processBypass(PlayerInstance player, BypassParser parser)
	{
		final int page = parser.getInt("page", 0);
		final int templateId = parser.getInt("id", 0);
		
		if (templateId > 0)
		{
			player.sendMessage("NOT DONE: detailed info about template ID: " + templateId);
		}
		else
		{
			sendTemplateList(player, page, parser);
		}
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}