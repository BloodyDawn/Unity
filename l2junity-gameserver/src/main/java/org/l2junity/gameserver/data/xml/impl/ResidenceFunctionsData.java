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
package org.l2junity.gameserver.data.xml.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.l2junity.gameserver.data.xml.IGameXmlReader;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.residences.ResidenceFunctionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author UnAfraid
 */
public final class ResidenceFunctionsData implements IGameXmlReader
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ResidenceFunctionsData.class);
	private final List<ResidenceFunctionTemplate> _functions = new ArrayList<>();
	
	protected ResidenceFunctionsData()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		_functions.clear();
		parseDatapackFile("data/ResidenceFunctions.xml");
		LOGGER.info("Loaded: {} functions.", _functions.size());
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		forEach(doc, "list", list -> forEach(list, "function", func ->
		{
			final NamedNodeMap attrs = func.getAttributes();
			final StatsSet set = new StatsSet(HashMap::new);
			for (int i = 0; i < attrs.getLength(); i++)
			{
				final Node node = attrs.item(i);
				set.set(node.getNodeName(), node.getNodeValue());
			}
			_functions.add(new ResidenceFunctionTemplate(set));
		}));
	}
	
	public ResidenceFunctionTemplate getFunctions(int id)
	{
		return _functions.stream().filter(func -> func.getId() == id).findFirst().orElse(null);
	}
	
	public static final ResidenceFunctionsData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ResidenceFunctionsData INSTANCE = new ResidenceFunctionsData();
	}
}
