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
package handlers.targethandlers;

import java.util.Comparator;
import java.util.List;

import org.l2junity.gameserver.handler.ITargetTypeHandler;
import org.l2junity.gameserver.model.L2Clan;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.L2TargetType;

/**
 * @author Sdw
 */
public class CorpsePartyClan implements ITargetTypeHandler
{
	@Override
	public Creature[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		if (activeChar.isPlayable())
		{
			final PlayerInstance player = activeChar.getActingPlayer();
			if (player == null)
			{
				return EMPTY_TARGET_LIST;
			}
			
			final L2Clan clan = player.getClan();
			final org.l2junity.gameserver.model.Party party = player.getParty();
			
			if ((clan != null) || (party != null))
			{
				final List<Creature> targetList = World.getInstance().getVisibleObjects(target, Creature.class, skill.getAffectRange(), o -> (o != activeChar) && checkTarget(activeChar, o));
				
				targetList.sort(Comparator.comparingDouble(p -> player.calculateDistance(p, false, false)));
				
				final int affectLimit = skill.getAffectLimit();
				
				if (targetList.size() > affectLimit)
				{
					targetList.subList(affectLimit, targetList.size()).clear();
				}
				
				return targetList.isEmpty() ? EMPTY_TARGET_LIST : targetList.toArray(new Creature[targetList.size()]);
			}
			
		}
		return EMPTY_TARGET_LIST;
	}
	
	private boolean checkTarget(Creature activeChar, Creature target)
	{
		if ((target.getActingPlayer() != null) && (target.getActingPlayer() != activeChar) && (target.getActingPlayer().inObserverMode() || target.getActingPlayer().isInOlympiadMode()))
		{
			return false;
		}
		
		if (target.isPlayable() && target.isDead())
		{
			if (activeChar == target)
			{
				return true;
			}
			
			final org.l2junity.gameserver.model.Party activeCharParty = activeChar.getParty();
			final org.l2junity.gameserver.model.Party targetParty = target.getParty();
			if ((activeCharParty != null) && (targetParty != null))
			{
				return (activeCharParty.getLeader() == targetParty.getLeader());
			}
			
			if ((activeChar.getClanId() != 0) && (target.getClanId() != 0))
			{
				return (activeChar.getClanId() == target.getClanId());
			}
		}
		return false;
	}
	
	@Override
	public Enum<L2TargetType> getTargetType()
	{
		return L2TargetType.CORPSE_PARTY_CLAN;
	}
}
