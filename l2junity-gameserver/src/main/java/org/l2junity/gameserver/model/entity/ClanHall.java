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
package org.l2junity.gameserver.model.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.l2junity.DatabaseFactory;
import org.l2junity.gameserver.data.sql.impl.ClanTable;
import org.l2junity.gameserver.data.xml.impl.ClanHallData;
import org.l2junity.gameserver.enums.ClanHallGrade;
import org.l2junity.gameserver.instancemanager.ZoneManager;
import org.l2junity.gameserver.model.L2Clan;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.instance.L2DoorInstance;
import org.l2junity.gameserver.model.zone.type.ClanHallZone;
import org.l2junity.gameserver.network.client.send.PledgeShowInfoUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClanHall extends AbstractResidence
{
	// Static parameters
	private final String _description;
	private final String _location;
	private final ClanHallGrade _grade;
	private final boolean _isAuctionable;
	private final List<L2DoorInstance> _doors;
	private final List<Integer> _npcs;
	private final Location _ownerLocation;
	private final Location _banishLocation;
	// Dynamic parameters
	private L2Clan _owner = null;
	private long _paidUntil = 0;
	private boolean _paid = false;
	// Other
	private static final String INSERT_CLANHALL = "INSERT INTO clanhall (id,ownerId,paidUntil,paid) VALUES (?,?,?,?)";
	private static final String LOAD_CLANHALL = "SELECT * FROM clanhall WHERE id=?";
	private static final String UPDATE_CLANHALL = "UPDATE clanhall SET ownerId=?,paidUntil=?,paid=? WHERE id=?";
	private static final Logger _log = LoggerFactory.getLogger(ClanHallData.class);
	
	public ClanHall(StatsSet params)
	{
		super(params.getInt("id"));
		// Set static parameters
		setName(params.getString("name"));
		_description = params.getString("description");
		_location = params.getString("location");
		_grade = params.getEnum("grade", ClanHallGrade.class);
		_isAuctionable = params.getBoolean("isauctionable");
		_doors = params.getList("doorList", L2DoorInstance.class);
		_npcs = params.getList("npcList", Integer.class);
		_ownerLocation = params.getLocation("owner_loc");
		_banishLocation = params.getLocation("banish_loc");
		// Set dynamic parameters (from DB)
		load();
	}
	
	@Override
	protected void load()
	{
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement loadStatement = con.prepareStatement(LOAD_CLANHALL);
			PreparedStatement insertStatement = con.prepareStatement(INSERT_CLANHALL))
		{
			loadStatement.setInt(1, getResidenceId());
			
			try (ResultSet rset = loadStatement.executeQuery())
			{
				if (rset.next())
				{
					setOwner(rset.getInt("ownerId"));
					setPaidUntil(rset.getLong("paidUntil"));
					setPaid(rset.getBoolean("paid"));
					_log.info("Clan Hall " + getName() + " (" + getResidenceId() + ") was sucessfully loaded.");
				}
				else
				{
					insertStatement.setInt(1, getResidenceId());
					insertStatement.setInt(2, 0); // New clanhall should not have owner
					insertStatement.setInt(3, 0); // New clanhall should not have paid until
					insertStatement.setString(4, "false"); // New clanhall should not have paid status
					if (insertStatement.execute())
					{
						_log.info("Clan Hall " + getName() + " (" + getResidenceId() + ") was sucessfully created.");
					}
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	public void updateDB()
	{
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_CLANHALL))
		{
			statement.setInt(1, getOwnerId());
			statement.setLong(2, getPaidUntil());
			statement.setBoolean(3, getPaid());
			statement.setInt(4, getResidenceId());
			statement.execute();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	protected void initResidenceZone()
	{
		final ClanHallZone zone = ZoneManager.getInstance().getAllZones(ClanHallZone.class).stream().filter(z -> z.getResidenceId() == getResidenceId()).findFirst().orElse(null);
		if (zone != null)
		{
			setResidenceZone(zone);
		}
	}
	
	public void openDoors()
	{
		openCloseDoors(true);
	}
	
	public void closeDoors()
	{
		openCloseDoors(false);
	}
	
	public void openCloseDoors(boolean open)
	{
		_doors.forEach(door ->
		{
			if (open)
			{
				door.openMe();
			}
			else
			{
				door.closeMe();
			}
		});
	}
	
	// ------------------------------------------------------------------------------
	
	/**
	 * Gets the grade of clan hall
	 * @return the grade of clan hall
	 */
	public ClanHallGrade getGrade()
	{
		return _grade;
	}
	
	/**
	 * Gets the doors of clan hall
	 * @return the doors of clan hall
	 */
	public List<L2DoorInstance> getDoors()
	{
		return _doors;
	}
	
	/**
	 * Gets the npcs of clan hall
	 * @return the npcs of clan hall
	 */
	public List<Integer> getNpcs()
	{
		return _npcs;
	}
	
	/**
	 * Gets the description of clan hall
	 * @return the description of clan hall
	 */
	public String getDescription()
	{
		return _description;
	}
	
	/**
	 * Gets the location of clan hall
	 * @return the location of clan hall
	 */
	public String getLocation()
	{
		return _location;
	}
	
	/**
	 * Gets the true/false for auction list of an clan hall
	 * @return {@code true} if the clan hall is auctionable, {@code false} otherwise.
	 */
	public boolean isAuctionable()
	{
		return _isAuctionable;
	}
	
	// ------------------------------------------------------------------------------
	
	/**
	 * Gets the owner of clan hall
	 * @return the owner of clan hall
	 */
	public L2Clan getOwner()
	{
		return _owner;
	}
	
	/**
	 * Gets the clan id of clan hall owner
	 * @return the clan id of clan hall owner
	 */
	public int getOwnerId()
	{
		final L2Clan owner = _owner;
		return (owner != null) ? owner.getId() : 0;
	}
	
	/**
	 * Set the owner of clan hall
	 * @param clanId the Id of the clan
	 */
	public void setOwner(int clanId)
	{
		setOwner(ClanTable.getInstance().getClan(clanId));
	}
	
	/**
	 * Set the clan as owner of clan hall
	 * @param clan the L2Clan object
	 */
	public void setOwner(L2Clan clan)
	{
		final L2Clan oldClan = getOwner();
		if ((clan == null) || (oldClan == clan))
		{
			return;
		}
		
		if (oldClan != null) // Update old clan
		{
			oldClan.setHideoutId(0);
			oldClan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(oldClan));
		}
		_owner = clan;
		clan.setHideoutId(getResidenceId());
		clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
		updateDB();
	}
	
	public void removeOwner()
	{
		final L2Clan clan = getOwner();
		if (clan == null)
		{
			return;
		}
		_owner = null;
		setPaid(false);
		setPaidUntil(0);
		clan.setHideoutId(0);
		clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
		updateDB();
	}
	
	// ------------------------------------------------------------------------------
	
	/**
	 * Gets the true/false for clan hall payment
	 * @return {@code true} if the clan hall is paid, {@code false} otherwise.
	 */
	public boolean getPaid()
	{
		return _paid;
	}
	
	/**
	 * Set the true/false if clan pay a fee for clan hall
	 * @param val the {@code true} when clan pay a fee, {@code false} otherwise.
	 */
	public void setPaid(boolean val)
	{
		_paid = val;
	}
	
	/**
	 * Gets the next date of clan hall payment
	 * @return the next date of clan hall payment
	 */
	public long getPaidUntil()
	{
		return _paidUntil;
	}
	
	/**
	 * Set the next date of clan hall payment
	 * @param paidUntil the date of next clan hall payment
	 */
	public void setPaidUntil(long paidUntil)
	{
		_paidUntil = paidUntil;
	}
	
	// ------------------------------------------------------------------------------
	
	public Location getOwnerLocation()
	{
		return _ownerLocation;
	}
	
	public Location getBanishLocation()
	{
		return _banishLocation;
	}
	
	@Override
	public String toString()
	{
		return (getClass().getSimpleName() + ":" + getName() + "[" + getResidenceId() + "]");
	}
}