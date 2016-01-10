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
package handlers.targethandlers.affectscope;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import org.l2junity.gameserver.handler.AffectObjectHandler;
import org.l2junity.gameserver.handler.IAffectObjectHandler;
import org.l2junity.gameserver.handler.IAffectScopeHandler;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.Playable;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.AffectScope;

/**
 * Party and Clan affect scope implementation.
 * @author Nik
 */
public class PartyPledge implements IAffectScopeHandler
{
	@Override
	public List<? extends WorldObject> getAffectedScope(Creature activeChar, Creature target, Skill skill)
	{
		final IAffectObjectHandler affectObject = AffectObjectHandler.getInstance().getHandler(skill.getAffectObject());
		final int affectRange = skill.getAffectRange();
		final int affectLimit = skill.getAffectLimit();
		final List<Creature> result = new LinkedList<>();
		
		if (target.isPlayable())
		{
			final PlayerInstance player = target.getActingPlayer();
			final Predicate<Playable> filter = plbl ->
			{
				PlayerInstance p = plbl.getActingPlayer();
				if ((p == null) || p.isDead())
				{
					return false;
				}
				if (p != player)
				{
					if ((p.getClanId() == 0) || (p.getClanId() != player.getClanId()))
					{
						return false;
					}
					if (!p.isInParty() || !player.isInParty() || (p.getParty().getLeaderObjectId() != player.getParty().getLeaderObjectId()))
					{
						return false;
					}
				}
				return ((affectObject == null) || affectObject.checkAffectedObject(activeChar, p));
			};
			
			// Add object of origin since its skipped in the forEachVisibleObjectInRange method.
			if (filter.test((Playable) target))
			{
				result.add(target);
			}
			
			// Check and add targets.
			World.getInstance().forEachVisibleObjectInRange(target, Playable.class, affectRange, c ->
			{
				if ((affectLimit > 0) && (result.size() >= affectLimit))
				{
					return;
				}
				if (filter.test(c))
				{
					result.add(c);
				}
			});
		}
		else if (target.isNpc())
		{
			final Predicate<Npc> filter = n ->
			{
				if (n.isDead())
				{
					return false;
				}
				if (!((Npc) target).isInMyClan(n) && n.isAutoAttackable(target))
				{
					return false;
				}
				return ((affectObject == null) || affectObject.checkAffectedObject(activeChar, n));
			};
			
			// Add object of origin since its skipped in the getVisibleObjects method.
			if (filter.test((Npc) target))
			{
				result.add(target);
			}
			
			// Check and add targets.
			World.getInstance().forEachVisibleObjectInRange(target, Npc.class, affectRange, c ->
			{
				if ((affectLimit > 0) && (result.size() >= affectLimit))
				{
					return;
				}
				if (filter.test(c))
				{
					result.add(c);
				}
			});
		}
		
		return result;
	}
	
	@Override
	public Enum<AffectScope> getAffectScopeType()
	{
		return AffectScope.PARTY_PLEDGE;
	}
}
