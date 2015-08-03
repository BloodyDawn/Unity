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
package handlers.effecthandlers;

import org.l2junity.gameserver.ai.CtrlIntention;
import org.l2junity.gameserver.data.xml.impl.NpcData;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.templates.L2NpcTemplate;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.interfaces.ILocational;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.network.client.send.FlyToLocation;
import org.l2junity.gameserver.network.client.send.FlyToLocation.FlyType;
import org.l2junity.gameserver.network.client.send.ValidateLocation;

/**
 * Escape to NPC effect implementation.
 * @author Nik
 */
public final class EscapeToNpc extends AbstractEffect
{
	private final int _npcId;
	private final boolean _summonedOnly; // Affect only NPCs summoned by you.
	
	public EscapeToNpc(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_npcId = params.getInt("npcId", 0);
		_summonedOnly = params.getBoolean("summonedOnly", true);
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.TELEPORT;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public boolean canStart(BuffInfo info)
	{
		// While affected by escape blocking effect you cannot use Blink or Scroll of Escape
		return !info.getEffected().cannotEscape();
	}

	@Override
	public void instant(Creature effector, Creature effected, Skill skill)
	{
		if (_npcId <= 0)
		{
			return;
		}
		
		final L2NpcTemplate template = NpcData.getInstance().getTemplate(_npcId);
		if (template == null)
		{
			return;
		}
		
		ILocational teleLocation = null;
		if (_summonedOnly)
		{
			// Search only summoned NPCs
			teleLocation = effector.getSummonedNpcs().stream().filter(npc -> npc.getId() == _npcId).findAny().orElse(null);
		}
		else
		{
			try
			{
				// Use the right NPC class for faster search.
				final Class<? extends Npc> clazz = Class.forName("org.l2junity.gameserver.model.actor.instance." + template.getType() + "Instance").asSubclass(Npc.class);
				final int range = skill.getCastRange() <= 0 ? Integer.MAX_VALUE : skill.getCastRange();
				
				for (Npc npc : World.getInstance().getVisibleObjects(effected, clazz, range))
				{
					if ((npc != null) && (npc.getId() == _npcId))
					{
						teleLocation = npc;
						break;
					}
				}
			}
			catch (ClassNotFoundException e)
			{
				_log.warn("Npc class not found: {} ", template.getType(), e);
			}
		}
		
		if (teleLocation != null)
		{
			if (effected.isInsideRadius(teleLocation, 900, false, false))
			{
				effected.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				effected.broadcastPacket(new FlyToLocation(effected, teleLocation, FlyType.DUMMY));
				effected.abortAttack();
				effected.abortCast();
				effected.setXYZ(teleLocation);
				effected.broadcastPacket(new ValidateLocation(effected));
			}
			else
			{
				effected.teleToLocation(teleLocation);
			}
		}
	}
}
