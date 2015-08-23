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
package handlers.bypasshandlers;

import org.l2junity.Config;
import org.l2junity.gameserver.data.sql.impl.NpcBufferTable;
import org.l2junity.gameserver.data.sql.impl.NpcBufferTable.NpcBufferData;
import org.l2junity.gameserver.handler.IBypassHandler;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.actor.instance.L2OlympiadManagerInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.olympiad.Olympiad;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.network.client.send.MagicSkillUse;
import org.l2junity.gameserver.network.client.send.NpcHtmlMessage;
import org.l2junity.gameserver.util.Util;

/**
 * @author DS
 */
public class OlympiadManagerLink implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"olybuff",
	};
	
	private static final int[] BUFFS =
	{
		14738, // Olympiad - Horn Melody
		14739, // Olympiad - Drum Melody
		14740, // Olympiad - Pipe Organ Melody
		14741, // Olympiad - Guitar Melody
		14742, // Olympiad - Harp Melody
		14743, // Olympiad - Lute Melody
		14744, // Olympiad - Knight's Harmony
		14745, // Olympiad - Warrior's Harmony
		14746, // Olympiad - Wizard's Harmony
	};
	
	@Override
	public final boolean useBypass(String command, PlayerInstance activeChar, Creature target)
	{
		if (!(target instanceof L2OlympiadManagerInstance))
		{
			return false;
		}
		
		try
		{
			if (command.toLowerCase().startsWith("olybuff"))
			{
				int buffCount = activeChar.getOlympiadBuffCount();
				if (buffCount <= 0)
				{
					return false;
				}
				
				final NpcHtmlMessage html = new NpcHtmlMessage(target.getObjectId());
				String[] params = command.split(" ");
				
				if (!Util.isDigit(params[1]))
				{
					_log.warn("Olympiad Buffer Warning: npcId = " + target.getId() + " has invalid buffGroup set in the bypass for the buff selected: " + params[1]);
					return false;
				}
				
				final int index = Integer.parseInt(params[1]);
				if ((index < 0) || (index > BUFFS.length))
				{
					_log.warn("Olympiad Buffer Warning: npcId = " + target.getId() + " has invalid index sent in the bypass: " + index);
					return false;
				}
				
				final NpcBufferData npcBuffGroupInfo = NpcBufferTable.getInstance().getSkillInfo(target.getId(), BUFFS[index]);
				if (npcBuffGroupInfo == null)
				{
					_log.warn("Olympiad Buffer Warning: npcId = " + target.getId() + " Location: " + target.getX() + ", " + target.getY() + ", " + target.getZ() + " Player: " + activeChar.getName() + " has tried to use skill group (" + params[1] + ") not assigned to the NPC Buffer!");
					return false;
				}
				
				if (buffCount > 0)
				{
					final Skill skill = npcBuffGroupInfo.getSkill().getSkill();
					if (skill != null)
					{
						target.setTarget(activeChar);
						
						activeChar.setOlympiadBuffCount(--buffCount);
						
						target.broadcastPacket(new MagicSkillUse(target, activeChar, skill.getId(), skill.getLevel(), 0, 0));
						skill.applyEffects(activeChar, activeChar);
						final Summon pet = activeChar.getPet();
						if (pet != null)
						{
							target.broadcastPacket(new MagicSkillUse(target, pet, skill.getId(), skill.getLevel(), 0, 0));
							skill.applyEffects(pet, pet);
						}
						activeChar.getServitors().values().forEach(s ->
						{
							target.broadcastPacket(new MagicSkillUse(target, s, skill.getId(), skill.getLevel(), 0, 0));
							skill.applyEffects(s, s);
						});
					}
				}
				
				if (buffCount > 0)
				{
					html.setFile(activeChar.getHtmlPrefix(), buffCount == Config.ALT_OLY_MAX_BUFFS ? Olympiad.OLYMPIAD_HTML_PATH + "olympiad_buffs.htm" : Olympiad.OLYMPIAD_HTML_PATH + "olympiad_5buffs.htm");
					html.replace("%objectId%", String.valueOf(target.getObjectId()));
					activeChar.sendPacket(html);
				}
				else
				{
					html.setFile(activeChar.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "olympiad_nobuffs.htm");
					html.replace("%objectId%", String.valueOf(target.getObjectId()));
					activeChar.sendPacket(html);
					target.decayMe();
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("Exception in " + getClass().getSimpleName(), e);
		}
		
		return true;
	}
	
	@Override
	public final String[] getBypassList()
	{
		return COMMANDS;
	}
}
