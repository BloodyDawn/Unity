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
package org.l2junity.gameserver.taskmanager.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.logging.Level;

import org.l2junity.Config;
import org.l2junity.DatabaseFactory;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.instance.L2PcInstance;
import org.l2junity.gameserver.model.actor.stat.PcStat;
import org.l2junity.gameserver.taskmanager.Task;
import org.l2junity.gameserver.taskmanager.TaskManager;
import org.l2junity.gameserver.taskmanager.TaskTypes;
import org.l2junity.gameserver.taskmanager.TaskManager.ExecutedTask;

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
		Calendar cal = Calendar.getInstance();
		if (cal.get(Calendar.DAY_OF_WEEK) == Config.ALT_VITALITY_DATE_RESET)
		{
			for (L2PcInstance player : World.getInstance().getPlayers())
			{
				player.setVitalityPoints(PcStat.MAX_VITALITY_POINTS, false);
			}
			
			try (Connection con = DatabaseFactory.getInstance().getConnection();
				PreparedStatement st = con.prepareStatement("DELETE FROM account_gsdata WHERE var = ?"))
			{
				st.setString(1, PcStat.VITALITY_VARIABLE);
				st.execute();
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "", e);
			}
			_log.info(getClass().getSimpleName() + ": launched.");
		}
	}
	
	@Override
	public void initializate()
	{
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_GLOBAL_TASK, "1", Config.ALT_VITALITY_HOUR_RESET, "");
	}
}
