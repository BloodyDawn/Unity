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
package org.l2junity.gameserver.model.actor.tasks.character;

import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.skills.Skill;

/**
 * Task dedicated to qued magic use of character
 * @author xban1x
 */
public final class QueuedMagicUseTask implements Runnable
{
	private final PlayerInstance _currPlayer;
	private final Skill _queuedSkill;
	private final boolean _isCtrlPressed;
	private final boolean _isShiftPressed;
	
	public QueuedMagicUseTask(PlayerInstance currPlayer, Skill queuedSkill, boolean isCtrlPressed, boolean isShiftPressed)
	{
		_currPlayer = currPlayer;
		_queuedSkill = queuedSkill;
		_isCtrlPressed = isCtrlPressed;
		_isShiftPressed = isShiftPressed;
	}
	
	@Override
	public void run()
	{
		if (_currPlayer != null)
		{
			_currPlayer.useMagic(_queuedSkill, _isCtrlPressed, _isShiftPressed);
		}
	}
}
