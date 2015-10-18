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

import org.l2junity.Config;
import org.l2junity.gameserver.data.sql.impl.ClanTable;
import org.l2junity.gameserver.model.L2Clan;
import org.l2junity.gameserver.taskmanager.Task;
import org.l2junity.gameserver.taskmanager.TaskManager;
import org.l2junity.gameserver.taskmanager.TaskManager.ExecutedTask;
import org.l2junity.gameserver.taskmanager.TaskTypes;

public class TaskDailyClanBonusReset extends Task
{
	private static final String NAME = "daily_world_chat_reset";
	
	@Override
	public String getName()
	{
		return NAME;
	}
	
	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		ClanTable.getInstance().getClans().forEach(L2Clan::resetClanBonus);
		LOGGER.info("Daily clan bonus has been resetted.");
	}
	
	@Override
	public void initializate()
	{
		TaskManager.addUniqueTask(getName(), TaskTypes.TYPE_GLOBAL_TASK, "1", Config.WORLD_CHAT_RESET_TIME, "");
	}
}
