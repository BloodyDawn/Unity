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

import java.util.List;

import org.l2junity.gameserver.handler.ITargetTypeHandler;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.L2SiegeFlagInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.L2TargetType;

/**
 * @author Sdw
 */
public class AuraFriendly implements ITargetTypeHandler
{
	@Override
	public Creature[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		List<Creature> targetList = World.getInstance().getVisibleObjects(target, Creature.class, skill.getAffectRange(), o -> checkTarget(activeChar, o, skill));
		final int affectLimit = skill.getAffectLimit();
		if ((affectLimit > 0) && (targetList.size() > affectLimit))
		{
			targetList.subList(affectLimit, targetList.size()).clear();
		}
		
		return targetList.isEmpty() ? EMPTY_TARGET_LIST : targetList.toArray(new Creature[targetList.size()]);
	}
	
	private boolean checkTarget(Creature activeChar, Creature target, Skill skill)
	{
		if ((target == null) || target.isAlikeDead() || target.isDoor() || (target instanceof L2SiegeFlagInstance) || target.isMonster())
		{
			return false;
		}
		
		if ((target.getActingPlayer() != null) && (target.getActingPlayer() != activeChar) && (target.getActingPlayer().inObserverMode() || target.getActingPlayer().isInOlympiadMode()))
		{
			return false;
		}
		
		if ((skill != null) && (skill.getAffectHeightMin() != 0) && (skill.getAffectHeightMax() != 0))
		{
			if (((activeChar.getZ() + skill.getAffectHeightMin()) > target.getZ()) || ((activeChar.getZ() + skill.getAffectHeightMax()) < target.getZ()))
			{
				return false;
			}
		}
		
		if (target.isPlayable())
		{
			boolean friendly = false;
			
			if (activeChar == target)
			{
				friendly = true;
			}
			else if ((activeChar.getAllyId() > 0) && (activeChar.getAllyId() == target.getActingPlayer().getAllyId()))
			{
				friendly = true;
			}
			else if ((activeChar.getClanId() > 0) && (activeChar.getClanId() == target.getActingPlayer().getClanId()))
			{
				friendly = true;
			}
			else if (activeChar.isInParty() && activeChar.getParty().containsPlayer(target.getActingPlayer()))
			{
				friendly = true;
			}
			else if ((target != activeChar) && (target.getActingPlayer().getPvpFlag() == 0))
			{
				friendly = true;
			}
			return friendly;
		}
		return true;
	}
	
	@Override
	public Enum<L2TargetType> getTargetType()
	{
		return L2TargetType.AURA_FRIENDLY;
	}
}
