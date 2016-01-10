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
package handlers.targethandlers;

import org.l2junity.gameserver.handler.ITargetTypeHandler;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Playable;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.TargetType;
import org.l2junity.gameserver.model.zone.ZoneId;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * Target dead player or pet.
 * @author Nik
 */
public class PcBody implements ITargetTypeHandler
{
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.PC_BODY;
	}
	
	@Override
	public WorldObject getTarget(Creature activeChar, Skill skill, boolean sendMessage)
	{
		final WorldObject target = activeChar.getTarget();
		if ((target != null) && (target.isPlayer() || target.isPet()))
		{
			final Playable targetPlayable = (Playable) target;
			if (targetPlayable.isDead())
			{
				if (targetPlayable.isPlayer() && targetPlayable.isInsideZone(ZoneId.SIEGE) && !targetPlayable.getActingPlayer().isInSiege())
				{
					// check target is not in a active siege zone
					if (sendMessage)
					{
						activeChar.sendPacket(SystemMessageId.IT_IS_NOT_POSSIBLE_TO_RESURRECT_IN_BATTLEGROUNDS_WHERE_A_SIEGE_WAR_IS_TAKING_PLACE);
					}
					
					return null;
				}
				
				return target;
			}
		}
		
		if (sendMessage)
		{
			activeChar.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
		}
		
		return null;
	}
}
