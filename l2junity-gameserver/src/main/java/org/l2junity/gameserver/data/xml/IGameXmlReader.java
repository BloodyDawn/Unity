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
package org.l2junity.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2junity.Config;
import org.l2junity.commons.util.IXmlReader;
import org.l2junity.gameserver.model.holders.MinionHolder;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Interface for XML parsers.
 * @author Zoey76
 */
public interface IGameXmlReader extends IXmlReader
{
	Logger LOGGER = LoggerFactory.getLogger(IGameXmlReader.class);

	/**
	 * Wrapper for {@link #parseFile(File)} method.
	 * @param path the relative path to the datapack root of the XML file to parse.
	 */
	default void parseDatapackFile(String path)
	{
		parseFile(new File(Config.DATAPACK_ROOT, path));
	}

	/**
	 * Wrapper for {@link #parseDirectory(File, boolean)}.
	 * @param path the path to the directory where the XML files are
	 * @param recursive parses all sub folders if there is
	 * @return {@code false} if it fails to find the directory, {@code true} otherwise
	 */
	default boolean parseDatapackDirectory(String path, boolean recursive)
	{
		return parseDirectory(new File(Config.DATAPACK_ROOT, path), recursive);
	}

	/**
	 * @param n
	 * @return a map of parameters
	 */
	default Map<String, Object> parseParameters(Node n)
	{
		final Map<String, Object> parameters = new HashMap<>();
		for (Node parameters_node = n.getFirstChild(); parameters_node != null; parameters_node = parameters_node.getNextSibling())
		{
			NamedNodeMap attrs = parameters_node.getAttributes();
			switch (parameters_node.getNodeName().toLowerCase())
			{
				case "param":
				{
					parameters.put(parseString(attrs, "name"), parseString(attrs, "value"));
					break;
				}
				case "skill":
				{
					parameters.put(parseString(attrs, "name"), new SkillHolder(parseInteger(attrs, "id"), parseInteger(attrs, "level")));
					break;
				}
				case "minions":
				{
					final List<MinionHolder> minions = new ArrayList<>(1);
					for (Node minions_node = parameters_node.getFirstChild(); minions_node != null; minions_node = minions_node.getNextSibling())
					{
						if (minions_node.getNodeName().equalsIgnoreCase("npc"))
						{
							attrs = minions_node.getAttributes();
							minions.add(new MinionHolder(parseInteger(attrs, "id"), parseInteger(attrs, "count"), parseInteger(attrs, "respawnTime"), parseInteger(attrs, "weightPoint")));
						}
					}
					
					if (!minions.isEmpty())
					{
						parameters.put(parseString(parameters_node.getAttributes(), "name"), minions);
					}
					break;
				}
			}
		}
		return parameters;
	}
}
