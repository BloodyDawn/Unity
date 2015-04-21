/*
 * Copyright (C) 2004-2014 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver.model.actor.tasks.cubics;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.L2CubicInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.network.client.send.MagicSkillUse;

/**
 * Cubic heal task.
 * @author Zoey76
 */
public class CubicBuff implements Runnable
{
	private static final Logger _log = Logger.getLogger(CubicBuff.class.getName());
	private final L2CubicInstance _cubic;
	private final int _chance;
	
	public CubicBuff(L2CubicInstance cubic, int chance)
	{
		_cubic = cubic;
		_chance = chance;
	}
	
	@Override
	public void run()
	{
		if (_cubic == null)
		{
			return;
		}
		
		if (_cubic.getOwner().isDead() || !_cubic.getOwner().isOnline())
		{
			_cubic.stopAction();
			_cubic.getOwner().getCubics().remove(_cubic.getId());
			_cubic.getOwner().broadcastUserInfo();
			_cubic.cancelDisappear();
			return;
		}
		
		if (Rnd.get(100) < _chance)
		{
			try
			{
				Skill skill = null;
				for (Skill sk : _cubic.getSkills())
				{
					if (sk.getId() == L2CubicInstance.SKILL_CUBIC_KNIGHT)
					{
						skill = sk;
						break;
					}
				}
				
				if (skill != null)
				{
					_cubic.cubicTargetForHeal();
					final Creature target = _cubic.getOwner();
					if ((target != null) && !target.isDead())
					{
						Creature[] targets =
						{
							target
						};
						
						skill.activateSkill(_cubic.getOwner(), targets);
						
						_cubic.getOwner().broadcastPacket(new MagicSkillUse(_cubic.getOwner(), target, skill.getId(), skill.getLevel(), 0, 0));
					}
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
}