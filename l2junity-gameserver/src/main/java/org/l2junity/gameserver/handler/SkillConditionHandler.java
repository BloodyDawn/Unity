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
package org.l2junity.gameserver.handler;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.skills.ISkillCondition;
import org.l2junity.gameserver.scripting.ScriptEngineManager;

/**
 * @author NosBit
 */
public final class SkillConditionHandler
{
	private static final Path SKILL_CONDITION_HANDLER_FILE = Paths.get(ScriptEngineManager.SCRIPT_FOLDER.toString(), "handlers", "SkillConditionMasterHandler.java");

	private final Map<String, Function<StatsSet, ISkillCondition>> _skillConditionHandlerFactories = new HashMap<>();

	public void registerHandler(String name, Function<StatsSet, ISkillCondition> handlerFactory)
	{
		_skillConditionHandlerFactories.put(name, handlerFactory);
	}

	public Function<StatsSet, ISkillCondition> getHandlerFactory(String name)
	{
		return _skillConditionHandlerFactories.get(name);
	}

	public int size()
	{
		return _skillConditionHandlerFactories.size();
	}

	public void executeScript()
	{
		try
		{

			ScriptEngineManager.getInstance().executeScript(SKILL_CONDITION_HANDLER_FILE);
		}
		catch (Exception e)
		{
			throw new Error("Problems while running SkillMasterHandler", e);
		}
	}
	
	private static final class SingletonHolder
	{
		protected static final SkillConditionHandler _instance = new SkillConditionHandler();
	}
	
	public static SkillConditionHandler getInstance()
	{
		return SingletonHolder._instance;
	}
}
