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
package org.l2junity.gameserver.model.entity;

import java.util.Calendar;
import java.util.List;

import org.l2junity.gameserver.model.L2Clan;
import org.l2junity.gameserver.model.SiegeClan;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;

/**
 * @author JIV
 */
public interface Siegable
{
	public void startSiege();
	
	public void endSiege();
	
	public SiegeClan getAttackerClan(int clanId);
	
	public SiegeClan getAttackerClan(L2Clan clan);
	
	public List<SiegeClan> getAttackerClans();
	
	public List<PlayerInstance> getAttackersInZone();
	
	public boolean checkIsAttacker(L2Clan clan);
	
	public SiegeClan getDefenderClan(int clanId);
	
	public SiegeClan getDefenderClan(L2Clan clan);
	
	public List<SiegeClan> getDefenderClans();
	
	public boolean checkIsDefender(L2Clan clan);
	
	public List<Npc> getFlag(L2Clan clan);
	
	public Calendar getSiegeDate();
	
	public boolean giveFame();
	
	public int getFameFrequency();
	
	public int getFameAmount();
	
	public void updateSiege();
}
