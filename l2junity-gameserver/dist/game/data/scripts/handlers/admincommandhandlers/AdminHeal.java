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

import java.util.Collection;
import java.util.logging.Logger;

import org.l2junity.Config;
import org.l2junity.gameserver.handler.IAdminCommandHandler;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * This class handles following admin commands: - heal = restores HP/MP/CP on target, name or radius
 * @version $Revision: 1.2.4.5 $ $Date: 2005/04/11 10:06:06 $ Small typo fix by Zoey76 24/02/2011
 */
public class AdminHeal implements IAdminCommandHandler
{
	private static Logger _log = Logger.getLogger(AdminRes.class.getName());
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_heal"
	};
	
	@Override
	public boolean useAdminCommand(String command, PlayerInstance activeChar)
	{
		
		if (command.equals("admin_heal"))
		{
			handleHeal(activeChar);
		}
		else if (command.startsWith("admin_heal"))
		{
			try
			{
				String healTarget = command.substring(11);
				handleHeal(activeChar, healTarget);
			}
			catch (StringIndexOutOfBoundsException e)
			{
				if (Config.DEVELOPER)
				{
					_log.warning("Heal error: " + e);
				}
				activeChar.sendMessage("Incorrect target/radius specified.");
			}
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void handleHeal(PlayerInstance activeChar)
	{
		handleHeal(activeChar, null);
	}
	
	private void handleHeal(PlayerInstance activeChar, String player)
	{
		
		WorldObject obj = activeChar.getTarget();
		if (player != null)
		{
			PlayerInstance plyr = World.getInstance().getPlayer(player);
			
			if (plyr != null)
			{
				obj = plyr;
			}
			else
			{
				try
				{
					int radius = Integer.parseInt(player);
					Collection<WorldObject> objs = activeChar.getKnownList().getKnownObjects().values();
					for (WorldObject object : objs)
					{
						if (object instanceof Creature)
						{
							Creature character = (Creature) object;
							character.setCurrentHpMp(character.getMaxHp(), character.getMaxMp());
							if (object instanceof PlayerInstance)
							{
								character.setCurrentCp(character.getMaxCp());
							}
						}
					}
					
					activeChar.sendMessage("Healed within " + radius + " unit radius.");
					return;
				}
				catch (NumberFormatException nbe)
				{
				}
			}
		}
		if (obj == null)
		{
			obj = activeChar;
		}
		if (obj instanceof Creature)
		{
			Creature target = (Creature) obj;
			target.setCurrentHpMp(target.getMaxHp(), target.getMaxMp());
			if (target instanceof PlayerInstance)
			{
				target.setCurrentCp(target.getMaxCp());
			}
			if (Config.DEBUG)
			{
				_log.fine("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ") healed character " + target.getName());
			}
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
		}
	}
}
