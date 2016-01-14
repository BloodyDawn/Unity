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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.l2junity.gameserver.handler.AffectObjectHandler;
import org.l2junity.gameserver.handler.IAffectObjectHandler;
import org.l2junity.gameserver.handler.IAffectScopeHandler;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.AffectScope;
import org.l2junity.gameserver.util.Util;

/**
 * @author Nik
 */
public class Party implements IAffectScopeHandler
{
	@Override
	public void forEachAffected(Creature activeChar, WorldObject target, Skill skill, Consumer<? super WorldObject> action)
	{
		final IAffectObjectHandler affectObject = AffectObjectHandler.getInstance().getHandler(skill.getAffectObject());
		final int affectRange = skill.getAffectRange();
		final int affectLimit = skill.getAffectLimit();
		
		if (target.isPlayable())
		{
			final PlayerInstance player = target.getActingPlayer();
			final org.l2junity.gameserver.model.Party party = player.getParty();
			final List<PlayerInstance> partyList = ((party != null) ? party.getMembers() : Collections.singletonList(player));
			
			//@formatter:off
			partyList.stream()
			.flatMap(p -> p.getServitors().values().stream())
			.flatMap(p -> Arrays.stream(new Creature[]{p.getPet()}))
			.filter(Objects::nonNull)
			.filter(c -> !c.isDead())
			.filter(c -> affectRange > 0 ? Util.checkIfInRange(affectRange, c, target, true) : true)
			.filter(c -> (affectObject == null) || affectObject.checkAffectedObject(activeChar, c))
			.limit(affectLimit > 0 ? affectLimit : Long.MAX_VALUE)
			.forEach(action);
			//@formatter:on
		}
		else if (target.isNpc())
		{
			final Npc npc = (Npc) target;
			
			// Create the target filter.
			final AtomicInteger affected = new AtomicInteger(0);
			final Predicate<Npc> filter = n ->
			{
				if ((affectLimit > 0) && (affected.get() >= affectLimit))
				{
					return false;
				}
				if (n.isDead())
				{
					return false;
				}
				if (n.isAutoAttackable(npc))
				{
					return false;
				}
				if ((affectObject != null) && !affectObject.checkAffectedObject(activeChar, n))
				{
					return false;
				}
				
				affected.incrementAndGet();
				return true;
			};
			
			// Add object of origin since its skipped in the getVisibleObjects method.
			if (filter.test(npc))
			{
				action.accept(npc);
			}
			
			// Check and add targets.
			World.getInstance().forEachVisibleObjectInRange(npc, Npc.class, affectRange, n ->
			{
				if (filter.test(n))
				{
					action.accept(n);
				}
			});
		}
	}
	
	@Override
	public Enum<AffectScope> getAffectScopeType()
	{
		return AffectScope.PARTY;
	}
}
