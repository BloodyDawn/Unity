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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2junity.DatabaseFactory;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.entity.Couple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author evill33t
 */
public final class CoupleManager
{
	private static final Logger _log = LoggerFactory.getLogger(CoupleManager.class);
	
	private volatile Map<Integer, Couple> _couples;
	
	protected CoupleManager()
	{
		load();
	}
	
	public void reload()
	{
		getCouples().clear();
		load();
	}
	
	private final void load()
	{
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			Statement ps = con.createStatement();
			ResultSet rs = ps.executeQuery("SELECT id FROM mods_wedding ORDER BY id"))
		{
			while (rs.next())
			{
				final int coupleId = rs.getInt("id");
				getCouples().put(coupleId, new Couple(coupleId));
			}
			_log.info(getClass().getSimpleName() + ": Loaded: " + getCouples().size() + " couples(s)");
		}
		catch (Exception e)
		{
			_log.error("Exception: CoupleManager.load(): " + e.getMessage(), e);
		}
	}
	
	public final Couple getCouple(int coupleId)
	{
		return getCouples().get(coupleId);
	}
	
	public void createCouple(PlayerInstance player1, PlayerInstance player2)
	{
		if ((player1 != null) && (player2 != null))
		{
			if ((player1.getPartnerId() == 0) && (player2.getPartnerId() == 0))
			{
				int _player1id = player1.getObjectId();
				int _player2id = player2.getObjectId();
				
				Couple _new = new Couple(player1, player2);
				getCouples().put(_new.getId(), _new);
				player1.setPartnerId(_player2id);
				player2.setPartnerId(_player1id);
				player1.setCoupleId(_new.getId());
				player2.setCoupleId(_new.getId());
			}
		}
	}
	
	public void deleteCouple(int coupleId)
	{
		Couple couple = getCouples().get(coupleId);
		if (couple != null)
		{
			PlayerInstance player1 = World.getInstance().getPlayer(couple.getPlayer1Id());
			PlayerInstance player2 = World.getInstance().getPlayer(couple.getPlayer2Id());
			if (player1 != null)
			{
				player1.setPartnerId(0);
				player1.setMarried(false);
				player1.setCoupleId(0);
				
			}
			if (player2 != null)
			{
				player2.setPartnerId(0);
				player2.setMarried(false);
				player2.setCoupleId(0);
				
			}
			couple.divorce();
			getCouples().remove(coupleId);
		}
	}
	
	public final Map<Integer, Couple> getCouples()
	{
		
		if (_couples == null)
		{
			synchronized (this)
			{
				if (_couples == null)
				{
					_couples = new ConcurrentHashMap<>();
				}
			}
		}
		return _couples;
	}
	
	public static final CoupleManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CoupleManager _instance = new CoupleManager();
	}
}
