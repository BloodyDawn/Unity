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

import org.l2junity.gameserver.GeoData;
import org.l2junity.gameserver.handler.ITargetTypeHandler;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.L2EventMonsterInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.TargetType;
import org.l2junity.gameserver.model.zone.ZoneId;
import org.l2junity.gameserver.network.client.send.FlyToLocation.FlyType;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * Any selected target.
 * @author Nik
 */
public class Target implements ITargetTypeHandler
{
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.TARGET;
	}
	
	@Override
	public WorldObject getTarget(Creature activeChar, WorldObject selectedTarget, Skill skill, boolean forceUse, boolean dontMove, boolean sendMessage)
	{
		final WorldObject target = activeChar.getTarget();
		// Check for null target or any other invalid target
		if ((target != null) && target.isCreature() && !((Creature) target).isDead())
		{
			if (!GeoData.getInstance().canSeeTarget(activeChar, target))
			{
				if (sendMessage)
				{
					activeChar.sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
				}
				return null;
			}
			
			if ((activeChar.isInsidePeaceZone(activeChar, target)) && !activeChar.getAccessLevel().allowPeaceAttack())
			{
				if (sendMessage)
				{
					activeChar.sendPacket(SystemMessageId.YOU_MAY_NOT_ATTACK_THIS_TARGET_IN_A_PEACEFUL_ZONE);
				}
				return null;
			}
			
			if ((skill.getFlyType() == FlyType.CHARGE) && !GeoData.getInstance().canMove(activeChar, target))
			{
				if (sendMessage)
				{
					activeChar.sendPacket(SystemMessageId.THE_TARGET_IS_LOCATED_WHERE_YOU_CANNOT_CHARGE);
				}
				return null;
			}
			
			if (activeChar.isPlayable() && target.isPlayable() && !activeChar.getActingPlayer().checkPvpSkill(target, skill) && !activeChar.getAccessLevel().allowPeaceAttack())
			{
				if (sendMessage)
				{
					activeChar.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
				}
				
				return null;
			}
			
			if (!target.isAutoAttackable(activeChar))
			{
				if (forceUse)
				{
					if ((target.getActingPlayer() != null) && (activeChar.getActingPlayer() != null))
					{
						if ((activeChar.getActingPlayer().getSiegeState() > 0) && activeChar.isInsideZone(ZoneId.SIEGE) && (target.getActingPlayer().getSiegeState() == activeChar.getActingPlayer().getSiegeState()) && (target.getActingPlayer() != activeChar.getActingPlayer()) && (target.getActingPlayer().getSiegeSide() == activeChar.getActingPlayer().getSiegeSide()))
						{
							if (sendMessage)
							{
								activeChar.sendPacket(SystemMessageId.FORCE_ATTACK_IS_IMPOSSIBLE_AGAINST_A_TEMPORARY_ALLIED_MEMBER_DURING_A_SIEGE);
							}
							return null;
						}
					}
					
					if (!target.canBeAttacked() && !activeChar.getAccessLevel().allowPeaceAttack() && !target.isDoor())
					{
						return null;
					}
				}
				else
				{
					return null;
				}
			}
			
			if ((target instanceof L2EventMonsterInstance) && ((L2EventMonsterInstance) target).eventSkillAttackBlocked())
			{
				return null;
			}
			
			// Check if the skill is a good magic, target is a monster and if force attack is set, if not then we don't want to cast.
			if ((skill.getEffectPoint() > 0) && target.isMonster() && !forceUse)
			{
				return null;
			}
			
			return target;
		}
		
		if (sendMessage)
		{
			activeChar.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
		}
		
		return null;
	}
}
