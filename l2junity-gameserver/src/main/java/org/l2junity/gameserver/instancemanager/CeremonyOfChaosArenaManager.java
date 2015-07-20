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
package org.l2junity.gameserver.instancemanager;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.ceremonyofchaos.CoCArena;
import org.l2junity.gameserver.model.ceremonyofchaos.CoCPlayer;
import org.l2junity.gameserver.model.holders.ItemHolder;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.network.client.send.NpcHtmlMessage;
import org.l2junity.gameserver.network.client.send.ceremonyofchaos.ExCuriousHouseEnter;
import org.l2junity.gameserver.network.client.send.ceremonyofchaos.ExCuriousHouseMemberList;

/**
 * @author Sdw
 */
public class CeremonyOfChaosArenaManager
{
	private static final String[] INSTANCE_TEMPLATES =
	{
		"CeremonyOfChaosArena1.xml",
		"CeremonyOfChaosArena2.xml",
		"CeremonyOfChaosArena3.xml",
		"CeremonyOfChaosArena4.xml"
	};
	
	private static final SkillHolder ENERGY_OF_CHAOS = new SkillHolder(7115, 1);
	private static final ItemHolder[] ITEMS = new ItemHolder[]
	{
		new ItemHolder(35991, 1),
		new ItemHolder(35992, 1),
		new ItemHolder(35993, 1),
	};
	
	public static final int MAX_PLAYERS = 18;
	
	private final Map<Integer, CoCArena> _arenas = new ConcurrentHashMap<>();
	
	protected CeremonyOfChaosArenaManager()
	{
	}
	
	public CoCArena createArena()
	{
		final String instanceTemplate = INSTANCE_TEMPLATES[Rnd.get(INSTANCE_TEMPLATES.length)];
		final CoCArena arena = new CoCArena(_arenas.size(), instanceTemplate);
		_arenas.put(arena.getId(), arena);
		return arena;
	}
	
	public Collection<CoCArena> getArenas()
	{
		return _arenas.values();
	}
	
	public CoCArena getArena(int id)
	{
		return _arenas.get(id);
	}
	
	public void startPreparation()
	{
		for (CoCArena arena : _arenas.values())
		{
			final ExCuriousHouseMemberList membersList = new ExCuriousHouseMemberList(arena.getId(), MAX_PLAYERS, arena.getPlayers());
			final NpcHtmlMessage msg = new NpcHtmlMessage(0);
			
			for (CoCPlayer cocPlayer : arena.getPlayers())
			{
				final PlayerInstance player = cocPlayer.getActiveChar();
				
				// Load the html
				msg.setFile(player.getHtmlPrefix(), "data/html/CeremonyOfChaos/started.htm");
				
				// Teleport player to the arena
				player.teleToLocation(arena.getInstance().getSpawnLoc(), arena.getInstance().getId(), 200);
				
				// Apply the Energy of Chaos skill
				ENERGY_OF_CHAOS.getSkill().activateSkill(player, player);
				
				// Send Enter packet
				player.sendPacket(ExCuriousHouseEnter.STATIC_PACKET);
				
				// Send all members
				player.sendPacket(membersList);
				
				// Send the entrance html
				player.sendPacket(msg);
				
				// Send support items to player
				for (ItemHolder item : ITEMS)
				{
					player.addItem("CoC", item, null, true);
				}
			}
		}
	}
	
	public void finish()
	{
		
	}
	
	public static CeremonyOfChaosArenaManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CeremonyOfChaosArenaManager _instance = new CeremonyOfChaosArenaManager();
	}
}
