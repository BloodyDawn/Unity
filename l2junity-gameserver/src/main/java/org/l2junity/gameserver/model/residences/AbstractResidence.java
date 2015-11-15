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
package org.l2junity.gameserver.model.residences;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2junity.DatabaseFactory;
import org.l2junity.gameserver.data.sql.impl.ClanTable;
import org.l2junity.gameserver.data.xml.impl.SkillTreesData;
import org.l2junity.gameserver.model.L2Clan;
import org.l2junity.gameserver.model.SkillLearn;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.events.ListenersContainer;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.model.interfaces.INamable;
import org.l2junity.gameserver.model.zone.type.ResidenceZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xban1x
 */
public abstract class AbstractResidence extends ListenersContainer implements INamable
{
	private final Logger LOGGER = LoggerFactory.getLogger(getClass());
	private final int _residenceId;
	private String _name;
	
	private ResidenceZone _zone = null;
	private final Map<Integer, ResidenceFunction> _functions = new ConcurrentHashMap<>();
	private final List<SkillHolder> _residentialSkills = new ArrayList<>();
	
	public AbstractResidence(int residenceId)
	{
		_residenceId = residenceId;
		initResidentialSkills();
	}
	
	protected abstract void load();
	
	protected abstract void initResidenceZone();
	
	public abstract int getOwnerId();
	
	protected void initResidentialSkills()
	{
		final List<SkillLearn> residentialSkills = SkillTreesData.getInstance().getAvailableResidentialSkills(getResidenceId());
		for (SkillLearn s : residentialSkills)
		{
			_residentialSkills.add(new SkillHolder(s.getSkillId(), s.getSkillLevel()));
		}
	}
	
	public final int getResidenceId()
	{
		return _residenceId;
	}
	
	@Override
	public final String getName()
	{
		return _name;
	}
	
	// TODO: Remove it later when both castles and forts are loaded from same table.
	public final void setName(String name)
	{
		_name = name;
	}
	
	public ResidenceZone getResidenceZone()
	{
		return _zone;
	}
	
	protected void setResidenceZone(ResidenceZone zone)
	{
		_zone = zone;
	}
	
	public final List<SkillHolder> getResidentialSkills()
	{
		return _residentialSkills;
	}
	
	public void giveResidentialSkills(PlayerInstance player)
	{
		if ((_residentialSkills != null) && !_residentialSkills.isEmpty())
		{
			for (SkillHolder sh : _residentialSkills)
			{
				player.addSkill(sh.getSkill(), false);
			}
		}
	}
	
	public void removeResidentialSkills(PlayerInstance player)
	{
		if ((_residentialSkills != null) && !_residentialSkills.isEmpty())
		{
			for (SkillHolder sh : _residentialSkills)
			{
				player.removeSkill(sh.getSkill(), false);
			}
		}
	}
	
	protected void initFunctions()
	{
		final L2Clan clan = ClanTable.getInstance().getClan(getOwnerId());
		if (clan == null)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM residense_functions WHERE ownerId = ? AND residenseId = ?"))
		{
			ps.setInt(1, getOwnerId());
			ps.setInt(2, getResidenceId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final int id = rs.getInt("id");
					final long expiration = rs.getLong("expiration");
					final ResidenceFunction func = new ResidenceFunction(id, expiration, getOwnerId(), this);
					if ((expiration <= System.currentTimeMillis()) && !func.reactivate())
					{
						continue;
					}
					_functions.put(id, func);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("Failed to initialize functions for owner: {} residense: {}", getOwnerId(), getResidenceId(), e);
		}
	}
	
	public void addFunction(ResidenceFunction func)
	{
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO residense_functions (id, expiration, ownerId, residenseId) VALUES (?, ?, ?, ?) ON DUPLICATE UPDATE expiration = ?, ownerId = ?"))
		{
			ps.setInt(1, func.getId());
			ps.setLong(2, func.getExpiration());
			ps.setInt(3, func.getOwnerId());
			ps.setInt(4, getResidenceId());
			
			ps.setLong(5, func.getExpiration());
			ps.setInt(6, func.getOwnerId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.warn("Failed to add function: {} for owner: {} residense: {}", func.getId(), getOwnerId(), getResidenceId(), e);
		}
		finally
		{
			final ResidenceFunction oldFunc = _functions.put(func.getId(), func);
			if (oldFunc != null)
			{
				removeFunction(oldFunc);
			}
		}
	}
	
	public void removeFunction(ResidenceFunction func)
	{
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE * FROM residense_functions WHERE ownerId = ? AND residenseId = ? and id = ?"))
		{
			ps.setInt(1, getOwnerId());
			ps.setInt(2, getResidenceId());
			ps.setInt(3, func.getId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.warn("Failed to remove function: {} for owner: {} residense: {}", func.getId(), getOwnerId(), getResidenceId(), e);
		}
		finally
		{
			func.cancelExpiration();
		}
	}
	
	public void removeFunctions()
	{
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE * FROM residense_functions WHERE ownerId = ? AND residenseId = ?"))
		{
			ps.setInt(1, getOwnerId());
			ps.setInt(2, getResidenceId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.warn("Failed to remove functions for owner: {} residense: {}", getOwnerId(), getResidenceId(), e);
		}
		finally
		{
			_functions.values().forEach(ResidenceFunction::cancelExpiration);
			_functions.clear();
		}
	}
	
	public boolean hasFunction(ResidenceFunctionType type)
	{
		return _functions.values().stream().map(ResidenceFunction::getTemplate).anyMatch(func -> func.getType() == type);
	}
	
	public ResidenceFunctionTemplate getFunction(ResidenceFunctionType type)
	{
		return _functions.values().stream().map(ResidenceFunction::getTemplate).filter(func -> func.getType() == type).findFirst().orElse(null);
	}
	
	public int getFunctionLevel(ResidenceFunctionType type)
	{
		return getFunction(type) != null ? getFunction(type).getLevel() : 0;
	}
	
	public long getFunctionExpiration(ResidenceFunctionType type)
	{
		final ResidenceFunction function = _functions.values().stream().filter(func -> func.getTemplate().getType() == type).findFirst().orElse(null);
		return function != null ? function.getExpiration() : -1;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return (obj instanceof AbstractResidence) && (((AbstractResidence) obj).getResidenceId() == getResidenceId());
	}
	
	@Override
	public String toString()
	{
		return getName() + " (" + getResidenceId() + ")";
	}
}
