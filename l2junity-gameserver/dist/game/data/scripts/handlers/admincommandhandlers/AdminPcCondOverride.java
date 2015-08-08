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

import org.l2junity.gameserver.handler.IAdminCommandHandler;
import org.l2junity.gameserver.model.PcCondOverride;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.client.send.NpcHtmlMessage;
import org.l2junity.gameserver.util.Util;

/**
 * Handler provides ability to override server's conditions for admin.<br>
 * Note: //setparam command uses any XML value and ignores case sensitivity.<br>
 * For best results by //setparam enable the maximum stats PcCondOverride here.
 * @author UnAfraid, Nik
 */
public class AdminPcCondOverride implements IAdminCommandHandler
{
	// private static final int SETPARAM_ORDER = 0x90;
	
	private static final String[] COMMANDS =
	{
		"admin_exceptions",
		"admin_set_exception",
		"admin_setparam",
		"admin_unsetparam",
		"admin_listparam",
		"admin_listparams"
	};
	
	@Override
	public boolean useAdminCommand(String command, PlayerInstance activeChar)
	{
		StringTokenizer st = new StringTokenizer(command);
		if (st.hasMoreTokens())
		{
			switch (st.nextToken())
			// command
			{
				case "admin_exceptions":
				{
					final NpcHtmlMessage msg = new NpcHtmlMessage(0, 1);
					msg.setFile(activeChar.getHtmlPrefix(), "data/html/admin/cond_override.htm");
					StringBuilder sb = new StringBuilder();
					for (PcCondOverride ex : PcCondOverride.values())
					{
						sb.append("<tr><td fixwidth=\"180\">" + ex.getDescription() + ":</td><td><a action=\"bypass -h admin_set_exception " + ex.ordinal() + "\">" + (activeChar.canOverrideCond(ex) ? "Disable" : "Enable") + "</a></td></tr>");
					}
					msg.replace("%cond_table%", sb.toString());
					activeChar.sendPacket(msg);
					break;
				}
				case "admin_set_exception":
				{
					if (st.hasMoreTokens())
					{
						String token = st.nextToken();
						if (Util.isDigit(token))
						{
							PcCondOverride ex = PcCondOverride.getCondOverride(Integer.valueOf(token));
							if (ex != null)
							{
								if (activeChar.canOverrideCond(ex))
								{
									activeChar.removeOverridedCond(ex);
									activeChar.sendMessage("You've disabled " + ex.getDescription());
								}
								else
								{
									activeChar.addOverrideCond(ex);
									activeChar.sendMessage("You've enabled " + ex.getDescription());
								}
							}
						}
						else
						{
							switch (token)
							{
								case "enable_all":
								{
									for (PcCondOverride ex : PcCondOverride.values())
									{
										if (!activeChar.canOverrideCond(ex))
										{
											activeChar.addOverrideCond(ex);
										}
									}
									activeChar.sendMessage("All condition exceptions have been enabled.");
									break;
								}
								case "disable_all":
								{
									for (PcCondOverride ex : PcCondOverride.values())
									{
										if (activeChar.canOverrideCond(ex))
										{
											activeChar.removeOverridedCond(ex);
										}
									}
									activeChar.sendMessage("All condition exceptions have been disabled.");
									break;
								}
							}
						}
						useAdminCommand(COMMANDS[0], activeChar);
					}
					break;
				}
					// case "admin_setparam":
					// {
					// try
					// {
					// Creature target = (Creature) (activeChar.getTarget() != null ? activeChar.getTarget() : null);
					// if (target == null)
					// {
					// target = activeChar;
					// }
					//
					// for (int i = 0; i < st.countTokens(); i += 2)
					// {
					// String statName = st.nextToken();
					// String value = st.nextToken();
					// try
					// {
					// Stats stat = valueOfXml(statName);
					// Calculator calc = target.getCalculators()[stat.ordinal()];
					//
					// // Remove old param.
					// if (calc != null)
					// {
					// for (AbstractFunction func : calc.getFunctions())
					// {
					// if (func.getOrder() == SETPARAM_ORDER)
					// {
					// calc.removeFunc(func);
					// }
					// }
					// }
					//
					// target.addStatFunc(new FuncSet(stat, SETPARAM_ORDER, target, Double.parseDouble(value), null));
					// activeChar.sendMessage("Stat: " + stat.getValue() + "(" + value + ") set to " + target.getName());
					// break;
					// }
					// catch (NoSuchElementException | NumberFormatException e)
					// {
					// activeChar.sendMessage("Incorrect stat name: " + statName + " with value: " + value);
					// }
					// }
					//
					// activeChar.broadcastUserInfo();
					// }
					// catch (Exception e)
					// {
					// activeChar.sendMessage("Usage: //setparam stat value; //setparam stat1 value1 stat2 value2 stat3 value3...");
					// activeChar.sendMessage("Error: " + e.getMessage());
					// }
					//
					// break;
					// }
					// case "admin_unsetparam":
					// {
					// Creature target = (Creature) (activeChar.getTarget() != null ? activeChar.getTarget() : null);
					// if (target == null)
					// {
					// target = activeChar;
					// }
					//
					// if (!st.hasMoreTokens())
					// {
					// for (Calculator calc : target.getCalculators())
					// {
					// if (calc == null)
					// {
					// continue;
					// }
					//
					// for (AbstractFunction func : calc.getFunctions())
					// {
					// if (func.getOrder() == SETPARAM_ORDER)
					// {
					// calc.removeFunc(func);
					// activeChar.sendMessage("Stat: " + func.getStat().getValue() + "(" + func.getValue() + ") removed from " + target.getName());
					// }
					// }
					// }
					//
					// activeChar.broadcastUserInfo();
					//
					// return true;
					// }
					//
					// List<Stats> stats = new LinkedList<>();
					// while (st.hasMoreTokens())
					// {
					// String statName = st.nextToken();
					// try
					// {
					// Stats stat = valueOfXml(statName);
					// stats.add(stat);
					// }
					// catch (Exception e)
					// {
					// activeChar.sendMessage("Stat with name: " + statName + " not found.");
					// }
					// }
					//
					// if (!stats.isEmpty())
					// {
					// for (Stats stat : stats)
					// {
					// final Calculator calc = target.getCalculators()[stat.ordinal()];
					// if (calc != null)
					// {
					// for (AbstractFunction func : calc.getFunctions())
					// {
					// if (func.getOrder() == SETPARAM_ORDER)
					// {
					// calc.removeFunc(func);
					// activeChar.sendMessage("Stat: " + func.getStat().getValue() + "(" + func.getValue() + ") removed from " + target.getName());
					// }
					// }
					//
					// }
					// else
					// {
					// activeChar.sendMessage("Calculator is null for stat: " + stat);
					// }
					// }
					//
					// activeChar.broadcastUserInfo();
					// }
					//
					// break;
					// }
					// case "admin_listparam":
					// case "admin_listparams":
					// {
					// try
					// {
					// Creature target = (Creature) (activeChar.getTarget() != null ? activeChar.getTarget() : null);
					// if (target == null)
					// {
					// target = activeChar;
					// }
					//
					// for (Calculator calc : target.getCalculators())
					// {
					// if (calc == null)
					// {
					// continue;
					// }
					//
					// for (AbstractFunction func : calc.getFunctions())
					// {
					// if (func.getOrder() == SETPARAM_ORDER)
					// {
					// activeChar.sendMessage("Stat: " + func.getStat().getValue() + "(" + func.getValue() + ")");
					// }
					// }
					// }
					//
					// return true;
					// }
					// catch (Exception e)
					// {
					// activeChar.sendMessage("Error: " + e.getMessage());
					// }
					//
					// break;
					// }
			}
		}
		return true;
	}
	
	// private static Stats valueOfXml(String name)
	// {
	// name = name.intern();
	// for (Stats s : Stats.values())
	// {
	// if (s.getValue().equalsIgnoreCase(name))
	// {
	// return s;
	// }
	// }
	//
	// throw new NoSuchElementException("Unknown name '" + name + "' for enum " + Stats.class.getSimpleName());
	// }
	//
	@Override
	public String[] getAdminCommandList()
	{
		return COMMANDS;
	}
}
