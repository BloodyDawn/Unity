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

import org.l2junity.gameserver.data.sql.impl.ClanTable;
import org.l2junity.gameserver.handler.IAdminCommandHandler;
import org.l2junity.gameserver.instancemanager.AuctionManager;
import org.l2junity.gameserver.instancemanager.CHSiegeManager;
import org.l2junity.gameserver.instancemanager.ClanHallManager;
import org.l2junity.gameserver.model.L2Clan;
import org.l2junity.gameserver.model.actor.instance.L2PcInstance;
import org.l2junity.gameserver.model.entity.ClanHall;
import org.l2junity.gameserver.model.entity.clanhall.SiegableHall;
import org.l2junity.gameserver.model.zone.type.ClanHallZone;
import org.l2junity.gameserver.network.SystemMessageId;
import org.l2junity.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2junity.gameserver.util.Util;

/**
 * This class handles Clan Hall commands.
 * @author Zoey76 (rework)
 */
public class AdminClanHall implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_clanhall",
		"admin_clanhallset",
		"admin_clanhalldel",
		"admin_clanhallopendoors",
		"admin_clanhallclosedoors",
		"admin_clanhallteleportself"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		command = st.nextToken(); // Get actual command
		
		ClanHall clanhall = null;
		if (st.hasMoreTokens())
		{
			L2PcInstance player = null;
			if ((activeChar.getTarget() != null) && activeChar.getTarget().isPlayer())
			{
				player = activeChar.getTarget().getActingPlayer();
			}
			
			String val = st.nextToken();
			if (command.startsWith("admin_clanhall"))
			{
				if (Util.isDigit(val))
				{
					clanhall = ClanHallManager.getInstance().getClanHallById(Integer.parseInt(val));
					L2Clan clan = null;
					switch (command)
					{
						case "admin_clanhallset":
							if ((player == null) || (player.getClan() == null))
							{
								activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
								return false;
							}
							
							if (clanhall.getOwnerId() > 0)
							{
								activeChar.sendMessage("This Clan Hall is not free!");
								return false;
							}
							
							clan = player.getClan();
							if (clan.getHideoutId() > 0)
							{
								activeChar.sendMessage("You have already a Clan Hall!");
								return false;
							}
							
							if (!clanhall.isSiegableHall())
							{
								ClanHallManager.getInstance().setOwner(clanhall.getId(), clan);
								if (AuctionManager.getInstance().getAuction(clanhall.getId()) != null)
								{
									AuctionManager.getInstance().getAuction(clanhall.getId()).deleteAuctionFromDB();
								}
							}
							else
							{
								clanhall.setOwner(clan);
								clan.setHideoutId(clanhall.getId());
							}
							break;
						case "admin_clanhalldel":
							
							if (!clanhall.isSiegableHall())
							{
								if (!ClanHallManager.getInstance().isFree(clanhall.getId()))
								{
									ClanHallManager.getInstance().setFree(clanhall.getId());
									AuctionManager.getInstance().initNPC(clanhall.getId());
								}
								else
								{
									activeChar.sendMessage("This Clan Hall is already free!");
								}
							}
							else
							{
								final int oldOwner = clanhall.getOwnerId();
								if (oldOwner > 0)
								{
									clanhall.free();
									clan = ClanTable.getInstance().getClan(oldOwner);
									if (clan != null)
									{
										clan.setHideoutId(0);
										clan.broadcastClanStatus();
									}
								}
							}
							break;
						case "admin_clanhallopendoors":
							clanhall.openCloseDoors(true);
							break;
						case "admin_clanhallclosedoors":
							clanhall.openCloseDoors(false);
							break;
						case "admin_clanhallteleportself":
							final ClanHallZone zone = clanhall.getZone();
							if (zone != null)
							{
								activeChar.teleToLocation(zone.getSpawnLoc(), true);
							}
							break;
						default:
							if (!clanhall.isSiegableHall())
							{
								showClanHallPage(activeChar, clanhall);
							}
							else
							{
								showSiegableHallPage(activeChar, (SiegableHall) clanhall);
							}
							break;
					}
				}
			}
		}
		else
		{
			showClanHallSelectPage(activeChar);
		}
		return true;
	}
	
	/**
	 * Show clan hall select page.
	 * @param activeChar the active char
	 */
	private void showClanHallSelectPage(L2PcInstance activeChar)
	{
		int i = 0;
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(0, 1);
		adminReply.setFile(activeChar.getHtmlPrefix(), "data/html/admin/clanhalls.htm");
		final StringBuilder cList = new StringBuilder(500);
		for (SiegableHall hall : CHSiegeManager.getInstance().getConquerableHalls().values())
		{
			if (hall != null)
			{
				cList.append("<td fixwidth=90><a action=\"bypass -h admin_chsiege_siegablehall ");
				cList.append(hall.getId());
				cList.append("\">");
				cList.append(hall.getName());
				cList.append("</a></td>");
				i++;
			}
			if (i > 1)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		adminReply.replace("%siegableHalls%", cList.toString());
		cList.setLength(0);
		i = 0;
		for (ClanHall clanhall : ClanHallManager.getInstance().getClanHalls().values())
		{
			if (clanhall != null)
			{
				cList.append("<td fixwidth=134><a action=\"bypass -h admin_clanhall ");
				cList.append(clanhall.getId());
				cList.append("\">");
				cList.append(clanhall.getName());
				cList.append("</a></td>");
				i++;
			}
			if (i > 1)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		adminReply.replace("%clanhalls%", cList.toString());
		cList.setLength(0);
		i = 0;
		for (ClanHall clanhall : ClanHallManager.getInstance().getFreeClanHalls().values())
		{
			if (clanhall != null)
			{
				cList.append("<td fixwidth=134><a action=\"bypass -h admin_clanhall ");
				cList.append(clanhall.getId());
				cList.append("\">");
				cList.append(clanhall.getName());
				cList.append("</a></td>");
				i++;
			}
			if (i > 1)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		adminReply.replace("%freeclanhalls%", cList.toString());
		activeChar.sendPacket(adminReply);
	}
	
	/**
	 * Show the clan hall page.
	 * @param activeChar the active char
	 * @param clanhall the clan hall
	 */
	private void showClanHallPage(L2PcInstance activeChar, ClanHall clanhall)
	{
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(0, 1);
		adminReply.setFile(activeChar.getHtmlPrefix(), "data/html/admin/clanhall.htm");
		adminReply.replace("%clanhallName%", clanhall.getName());
		adminReply.replace("%clanhallId%", String.valueOf(clanhall.getId()));
		final L2Clan owner = ClanTable.getInstance().getClan(clanhall.getOwnerId());
		adminReply.replace("%clanhallOwner%", (owner == null) ? "None" : owner.getName());
		activeChar.sendPacket(adminReply);
	}
	
	/**
	 * Show the siegable hall page.
	 * @param activeChar the active char
	 * @param hall the siegable hall
	 */
	private void showSiegableHallPage(L2PcInstance activeChar, SiegableHall hall)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage(0, 1);
		msg.setFile(null, "data/html/admin/siegablehall.htm");
		msg.replace("%clanhallId%", String.valueOf(hall.getId()));
		msg.replace("%clanhallName%", hall.getName());
		if (hall.getOwnerId() > 0)
		{
			final L2Clan owner = ClanTable.getInstance().getClan(hall.getOwnerId());
			msg.replace("%clanhallOwner%", (owner != null) ? owner.getName() : "No Owner");
		}
		else
		{
			msg.replace("%clanhallOwner%", "No Owner");
		}
		activeChar.sendPacket(msg);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
