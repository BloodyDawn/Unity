/*
 * Copyright (C) 2004-2015 L2J Server
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
package org.l2junity.gameserver.model.effects;

/**
 * @author UnAfraid
 */
public enum EffectFlag
{
	NONE,
	RESURRECTION_SPECIAL,
	NOBLESS_BLESSING,
	SILENT_MOVE,
	PROTECTION_BLESSING,
	RELAXING,
	FEAR,
	CONFUSED,
	MUTED,
	PSYCHICAL_MUTED,
	PSYCHICAL_ATTACK_MUTED,
	DISARMED,
	ROOTED,
	SLEEP,
	STUNNED,
	BETRAYED,
	INVUL,
	PARALYZED,
	BLOCK_RESURRECTION,
	SERVITOR_SHARE,
	UNTARGETABLE,
	CANNOT_ESCAPE,
<<<<<<< Upstream, based on branch 'master' of https://nik--@github.com/UnAfraid/L2JUnity
	DEBUFF_BLOCK;
=======
	FIRE_STANCE,
	WATER_STANCE,
	WIND_STANCE,
	EARTH_STANCE;
>>>>>>> ba7a782 The following effects have been fixed to match retail: DamOverTime - upon magic critical does 10x the DOT damage. Fear - Stops target when it ends and it doesnt change you to running state when it lands (you can walk and be feared). Also, as far as I saw, you run away from target only once and its upon landing. After that even if you are infront of the feared player, it doesnt run away in the opposite direction. Finally, you run until fear ends, there is no range limit of how much you can run from target. Also, I've notice fear getting removed when you get hit (need to confirm). ResetDebuff - there are cases where it resets only specific types of debuffs.
	
	public int getMask()
	{
		return 1 << ordinal();
	}
}
