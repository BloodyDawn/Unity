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

import org.l2junity.Config;
import org.l2junity.gameserver.handler.ITargetTypeHandler;
import org.l2junity.gameserver.model.actor.Attackable;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.targets.L2TargetType;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * Corpse Mob target handler.
 * @author UnAfraid, Zoey76
 */
public class CorpseMob implements ITargetTypeHandler
{
	@Override
	public Creature[] getTargetList(Skill skill, Creature activeChar, boolean onlyFirst, Creature target)
	{
		if ((target == null) || !target.isAttackable() || !target.isDead())
		{
			activeChar.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
			return EMPTY_TARGET_LIST;
		}
		
		if (skill.hasEffectType(L2EffectType.SUMMON) && target.isServitor() && (target.getActingPlayer() != null) && (target.getActingPlayer().getObjectId() == activeChar.getObjectId()))
		{
			return EMPTY_TARGET_LIST;
		}
		
		if (skill.hasEffectType(L2EffectType.HP_DRAIN) && ((Attackable) target).isOldCorpse(activeChar.getActingPlayer(), Config.CORPSE_CONSUME_SKILL_ALLOWED_TIME_BEFORE_DECAY, true))
		{
			return EMPTY_TARGET_LIST;
		}
		
		return new Creature[]
		{
			target
		};
	}
	
	@Override
	public Enum<L2TargetType> getTargetType()
	{
		return L2TargetType.CORPSE_MOB;
	}
}
