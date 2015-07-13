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

import java.util.StringTokenizer;

import org.l2junity.gameserver.datatables.SpawnTable;
import org.l2junity.gameserver.handler.IAdminCommandHandler;
import org.l2junity.gameserver.instancemanager.RaidBossSpawnManager;
import org.l2junity.gameserver.model.L2Spawn;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.html.PageResult;
import org.l2junity.gameserver.model.html.pagehandlers.NextPrevPageHandler;
import org.l2junity.gameserver.model.html.styles.ButtonsStyle;
import org.l2junity.gameserver.network.client.send.NpcHtmlMessage;
import org.l2junity.gameserver.util.HtmlUtil;
import org.l2junity.gameserver.util.Util;

/**
 * @author NosBit
 */
public class AdminScan implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_scan",
		"admin_deleteNpcByObjectId"
	};
	
	private static final int DEFAULT_RADIUS = 500;
	
	@Override
	public boolean useAdminCommand(String command, PlayerInstance activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		switch (actualCommand.toLowerCase())
		{
			case "admin_scan":
			{
				int radius = DEFAULT_RADIUS;
				int page = 0;
				if (st.hasMoreTokens())
				{
					try
					{
						radius = Integer.parseInt(st.nextToken());
					}
					catch (NumberFormatException e)
					{
						activeChar.sendMessage("Usage: //scan [radius]");
						return false;
					}
				}
				
				if (st.hasMoreTokens())
				{
					try
					{
						page = Integer.parseInt(st.nextToken());
					}
					catch (NumberFormatException e)
					{
					}
				}
				
				sendNpcList(activeChar, radius, page);
				break;
			}
			case "admin_deletenpcbyobjectid":
			{
				if (!st.hasMoreElements())
				{
					activeChar.sendMessage("Usage: //deletenpcbyobjectid <object_id>");
					return false;
				}
				int page = 0;
				try
				{
					int objectId = Integer.parseInt(st.nextToken());
					
					try
					{
						page = Integer.parseInt(st.nextToken());
					}
					catch (NumberFormatException e)
					{
					}

					final WorldObject target = World.getInstance().findObject(objectId);
					final Npc npc = target instanceof Npc ? (Npc) target : null;
					if (npc == null)
					{
						activeChar.sendMessage("NPC does not exist or object_id does not belong to an NPC");
						return false;
					}
					
					npc.deleteMe();
					
					final L2Spawn spawn = npc.getSpawn();
					if (spawn != null)
					{
						spawn.stopRespawn();
						
						if (RaidBossSpawnManager.getInstance().isDefined(spawn.getId()))
						{
							RaidBossSpawnManager.getInstance().deleteSpawn(spawn, true);
						}
						else
						{
							SpawnTable.getInstance().deleteSpawn(spawn, true);
						}
					}
					
					activeChar.sendMessage(npc.getName() + " have been deleted.");
				}
				catch (NumberFormatException e)
				{
					activeChar.sendMessage("object_id must be a number.");
					return false;
				}
				
				sendNpcList(activeChar, DEFAULT_RADIUS, page);
				break;
			}
		}
		return true;
	}
	
	private void sendNpcList(PlayerInstance activeChar, int radius, int page)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(activeChar.getHtmlPrefix(), "data/html/admin/scan.htm");
		final PageResult result = HtmlUtil.createPage(World.getInstance().getVisibleObjects(activeChar, Npc.class, radius), page, 15, new NextPrevPageHandler(page, "bypass -h admin_scan " + radius, ButtonsStyle.INSTANCE), (pages, character, sb) ->
		{
			sb.append("<tr>");
			sb.append("<td width=\"45\">" + character.getId() + "</td>");
			sb.append("<td><a action=\"bypass -h admin_move_to " + character.getX() + " " + character.getY() + " " + character.getZ() + "\">" + character.getName() + "</a></td>");
			sb.append("<td width=\"60\">" + Util.formatAdena(Math.round(activeChar.calculateDistance(character, false, false))) + "</td>");
			sb.append("<td width=\"54\"><a action=\"bypass -h admin_deleteNpcByObjectId " + character.getObjectId() + "\"><font color=\"LEVEL\">Delete</font></a></td>");
			sb.append("</tr>");
		});
		
		if (result.getPages() > 0)
		{
			html.replace("%pages%", "<center><table width=\"100%\" cellspacing=0><tr>" + result.getPagerTemplate() + "</tr></table></center>");
		}
		else
		{
			html.replace("%pages%", "");
		}

		html.replace("%data%", result.getBodyTemplate().toString());
		activeChar.sendPacket(html);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
