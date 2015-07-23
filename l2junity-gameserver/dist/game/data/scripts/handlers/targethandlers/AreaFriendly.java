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

import java.util.Comparator;
import java.util.List;

import org.l2junity.gameserver.handler.ITargetTypeHandler;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.L2SiegeFlagInstance;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.L2TargetType;
import org.l2junity.gameserver.model.zone.ZoneId;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * @author Adry_85
 */
public class AreaFriendly implements ITargetTypeHandler
{
	@Override
	public Creature[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		if (!checkTarget(activeChar, target) && (skill.getCastRange() >= 0))
		{
			activeChar.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
			return EMPTY_TARGET_LIST;
		}
		
		if (onlyFirst)
		{
			return new Creature[]
			{
				target
			};
		}
		
		if (activeChar.getActingPlayer().isInOlympiadMode())
		{
			return new Creature[]
			{
				activeChar
			};
		}
		
		if (target != null)
		{
			List<Creature> targetList = World.getInstance().getVisibleObjects(target, Creature.class, skill.getAffectRange(), o -> checkTarget(activeChar, o) && (o != activeChar));
			if (skill.hasEffectType(L2EffectType.HEAL))
			{
				targetList.sort(new CharComparator());
			}
			targetList.add(0, target);
			final int affectLimit = skill.getAffectLimit();
			if (targetList.size() > affectLimit)
			{
				targetList.subList(affectLimit, targetList.size()).clear();
			}
			
			return targetList.isEmpty() ? EMPTY_TARGET_LIST : targetList.toArray(new Creature[targetList.size()]);
		}
		
		return EMPTY_TARGET_LIST;
	}
	
	private boolean checkTarget(Creature activeChar, Creature target)
	{
		if ((target == null) || target.isAlikeDead() || target.isDoor() || (target instanceof L2SiegeFlagInstance) || target.isMonster())
		{
			return false;
		}
		
		if ((target.getActingPlayer() != null) && (target.getActingPlayer() != activeChar) && (target.getActingPlayer().inObserverMode() || target.getActingPlayer().isInOlympiadMode()))
		{
			return false;
		}
		
		if (target.isPlayable())
		{
			if (activeChar == target)
			{
				return true;
			}
			
			if ((target != activeChar) && activeChar.isInParty() && target.isInParty())
			{
				return (activeChar.getParty().getLeader() == target.getParty().getLeader());
			}
			
			if ((activeChar.getClanId() != 0) && (target.getClanId() != 0))
			{
				return (activeChar.getClanId() == target.getClanId());
			}
			
			if ((activeChar.getAllyId() != 0) && (target.getAllyId() != 0))
			{
				return (activeChar.getAllyId() == target.getAllyId());
			}
			
			if ((target != activeChar) && ((target.getActingPlayer().getPvpFlag() > 0) || (target.getActingPlayer().getReputation() < 0)))
			{
				return false;
			}
			
			if (target.isInsideZone(ZoneId.PVP))
			{
				return false;
			}
		}
		return true;
	}
	
	public class CharComparator implements Comparator<Creature>
	{
		@Override
		public int compare(Creature char1, Creature char2)
		{
			return Double.compare((char1.getCurrentHp() / char1.getMaxHp()), (char2.getCurrentHp() / char2.getMaxHp()));
		}
	}
	
	@Override
	public Enum<L2TargetType> getTargetType()
	{
		return L2TargetType.AREA_FRIENDLY;
	}
}
