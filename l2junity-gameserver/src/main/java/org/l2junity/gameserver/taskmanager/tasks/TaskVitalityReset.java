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
package org.l2junity.gameserver.taskmanager.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;

import org.l2junity.Config;
import org.l2junity.DatabaseFactory;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.actor.stat.PcStat;
import org.l2junity.gameserver.model.base.SubClass;
import org.l2junity.gameserver.taskmanager.Task;
import org.l2junity.gameserver.taskmanager.TaskManager;
import org.l2junity.gameserver.taskmanager.TaskManager.ExecutedTask;
import org.l2junity.gameserver.taskmanager.TaskTypes;

/**
 * @author UnAfraid
 */
public class TaskVitalityReset extends Task
{
	private static final String NAME = "vitalityreset";
	
	@Override
	public String getName()
	{
		return NAME;
	}
	
	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		final LocalDate now = LocalDate.now();
		for (int day : Config.ALT_VITALITY_DATE_RESET)
		{
			if (now.getDayOfWeek().getValue() == day)
			{
				for (PlayerInstance player : World.getInstance().getPlayers())
				{
					player.setVitalityPoints(PcStat.MAX_VITALITY_POINTS, false);
					
					for (SubClass subclass : player.getSubClasses().values())
					{
						subclass.setVitalityPoints(PcStat.MAX_VITALITY_POINTS);
					}
				}
				
				try (Connection con = DatabaseFactory.getInstance().getConnection();
					PreparedStatement st = con.prepareStatement("UPDATE character_subclasses SET vitality_points = ?");
					PreparedStatement st2 = con.prepareStatement("UPDATE characters SET vitality_points = ?"))
				{
					st.setInt(1, PcStat.MAX_VITALITY_POINTS);
					st.execute();
					
					st2.setInt(1, PcStat.MAX_VITALITY_POINTS);
					st2.execute();
				}
				catch (Exception e)
				{
					_log.warn("", e);
				}
				_log.info("Vitality resetted");
				break;
			}
		}
	}
	
	@Override
	public void initializate()
	{
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_GLOBAL_TASK, "1", Config.ALT_VITALITY_HOUR_RESET, "");
	}
}
