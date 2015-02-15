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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2junity.DatabaseFactory;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.instance.L2PcInstance;
import org.l2junity.gameserver.model.entity.Couple;

import javolution.util.FastList;

/**
 * @author evill33t
 */
public final class CoupleManager
{
	private static final Logger _log = Logger.getLogger(CoupleManager.class.getName());
	
	private FastList<Couple> _couples;
	
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
				getCouples().add(new Couple(rs.getInt("id")));
			}
			_log.info(getClass().getSimpleName() + ": Loaded: " + getCouples().size() + " couples(s)");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Exception: CoupleManager.load(): " + e.getMessage(), e);
		}
	}
	
	public final Couple getCouple(int coupleId)
	{
		int index = getCoupleIndex(coupleId);
		if (index >= 0)
		{
			return getCouples().get(index);
		}
		return null;
	}
	
	public void createCouple(L2PcInstance player1, L2PcInstance player2)
	{
		if ((player1 != null) && (player2 != null))
		{
			if ((player1.getPartnerId() == 0) && (player2.getPartnerId() == 0))
			{
				int _player1id = player1.getObjectId();
				int _player2id = player2.getObjectId();
				
				Couple _new = new Couple(player1, player2);
				getCouples().add(_new);
				player1.setPartnerId(_player2id);
				player2.setPartnerId(_player1id);
				player1.setCoupleId(_new.getId());
				player2.setCoupleId(_new.getId());
			}
		}
	}
	
	public void deleteCouple(int coupleId)
	{
		int index = getCoupleIndex(coupleId);
		Couple couple = getCouples().get(index);
		if (couple != null)
		{
			L2PcInstance player1 = World.getInstance().getPlayer(couple.getPlayer1Id());
			L2PcInstance player2 = World.getInstance().getPlayer(couple.getPlayer2Id());
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
			getCouples().remove(index);
		}
	}
	
	public final int getCoupleIndex(int coupleId)
	{
		int i = 0;
		for (Couple temp : getCouples())
		{
			if ((temp != null) && (temp.getId() == coupleId))
			{
				return i;
			}
			i++;
		}
		return -1;
	}
	
	public final FastList<Couple> getCouples()
	{
		if (_couples == null)
		{
			_couples = new FastList<>();
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
