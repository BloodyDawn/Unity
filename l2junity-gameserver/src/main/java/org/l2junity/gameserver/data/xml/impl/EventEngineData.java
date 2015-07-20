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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.l2junity.gameserver.data.xml.IGameXmlReader;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.eventengine.AbstractEventManager;
import org.l2junity.gameserver.model.eventengine.EventMethodNotification;
import org.l2junity.gameserver.model.eventengine.EventScheduler;
import org.l2junity.gameserver.model.holders.ItemHolder;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author UnAfraid
 */
public class EventEngineData implements IGameXmlReader
{
	private static final Logger LOGGER = LoggerFactory.getLogger(EventEngineData.class);
	
	protected EventEngineData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackDirectory("data/events", true);
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		for (Node listNode = doc.getFirstChild(); listNode != null; listNode = listNode.getNextSibling())
		{
			if ("list".equals(listNode.getNodeName()))
			{
				for (Node eventNode = listNode.getFirstChild(); eventNode != null; eventNode = eventNode.getNextSibling())
				{
					if ("event".equals(eventNode.getNodeName()))
					{
						parseEvent(eventNode);
					}
				}
			}
		}
	}
	
	/**
	 * @param eventNode
	 */
	private void parseEvent(Node eventNode)
	{
		final String eventName = parseString(eventNode.getAttributes(), "name");
		final String className = parseString(eventNode.getAttributes(), "class");
		AbstractEventManager<?> eventManager = null;
		try
		{
			final Class<?> clazz = Class.forName(className);
			
			// Attempt to find getInstance() method
			for (Method method : clazz.getMethods())
			{
				if (Modifier.isStatic(method.getModifiers()) && AbstractEventManager.class.isAssignableFrom(method.getReturnType()) && (method.getParameterCount() == 0))
				{
					eventManager = (AbstractEventManager<?>) method.invoke(null);
					break;
				}
			}
			
			if (eventManager == null)
			{
				throw new NoSuchMethodError("Couldn't method that gives instance of AbstractEventManager!");
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("Couldn't locate event manager instance for event: {} !", eventName, e);
			return;
		}
		
		for (Node innerNode = eventNode.getFirstChild(); innerNode != null; innerNode = innerNode.getNextSibling())
		{
			if ("variables".equals(innerNode.getNodeName()))
			{
				parseVariables(eventManager, innerNode);
			}
			else if ("scheduler".equals(innerNode.getNodeName()))
			{
				parseScheduler(eventManager, innerNode);
			}
		}
		
		// Start the scheduler
		eventManager.startScheduler();
		
		// Notify the event manager that we've done initializing its stuff
		eventManager.onInitialized();
		
		LOGGER.info("{}: Initialized", eventManager.getClass().getSimpleName());
	}
	
	/**
	 * @param eventManager
	 * @param innerNode
	 */
	private void parseVariables(AbstractEventManager<?> eventManager, Node innerNode)
	{
		eventManager.getVariables().getSet().clear();
		for (Node variableNode = innerNode.getFirstChild(); variableNode != null; variableNode = variableNode.getNextSibling())
		{
			if ("variable".equals(variableNode.getNodeName()))
			{
				eventManager.getVariables().set(parseString(variableNode.getAttributes(), "name"), parseString(variableNode.getAttributes(), "value"));
			}
			else if ("list".equals(variableNode.getNodeName()))
			{
				parseListVariables(eventManager, variableNode);
			}
		}
	}
	
	/**
	 * @param eventManager
	 * @param innerNode
	 */
	private void parseScheduler(AbstractEventManager<?> eventManager, Node innerNode)
	{
		eventManager.getSchedulers().clear();
		for (Node scheduleNode = innerNode.getFirstChild(); scheduleNode != null; scheduleNode = scheduleNode.getNextSibling())
		{
			if ("schedule".equals(scheduleNode.getNodeName()))
			{
				final StatsSet params = new StatsSet(LinkedHashMap::new);
				final NamedNodeMap attrs = scheduleNode.getAttributes();
				for (int i = 0; i < attrs.getLength(); i++)
				{
					final Node node = attrs.item(i);
					params.set(node.getNodeName(), node.getNodeValue());
				}
				
				final EventScheduler scheduler = new EventScheduler(eventManager, params);
				for (Node eventNode = scheduleNode.getFirstChild(); eventNode != null; eventNode = eventNode.getNextSibling())
				{
					if ("event".equals(eventNode.getNodeName()))
					{
						String methodName = parseString(eventNode.getAttributes(), "name");
						if (methodName.charAt(0) == '#')
						{
							methodName = methodName.substring(1);
						}
						
						final List<Object> args = new ArrayList<>();
						for (Node argsNode = eventNode.getFirstChild(); argsNode != null; argsNode = argsNode.getNextSibling())
						{
							if ("arg".equals(argsNode.getNodeName()))
							{
								final Object value = parseArg(eventManager, argsNode);
								if (value != null)
								{
									args.add(value);
								}
							}
						}
						
						try
						{
							scheduler.addEventNotification(new EventMethodNotification(eventManager, methodName, args));
						}
						catch (Exception e)
						{
							LOGGER.warn("Couldn't add event notification for {}", eventManager.getClass().getSimpleName());
						}
					}
				}
				eventManager.getSchedulers().add(scheduler);
			}
		}
	}
	
	/**
	 * @param eventManager
	 * @param variableNode
	 */
	private void parseListVariables(AbstractEventManager<?> eventManager, Node variableNode)
	{
		final String name = parseString(variableNode.getAttributes(), "name");
		final String type = parseString(variableNode.getAttributes(), "type");
		switch (type)
		{
			case "Byte":
			{
				final List<Byte> bytes = new ArrayList<>();
				for (Node stringNode = variableNode.getFirstChild(); stringNode != null; stringNode = stringNode.getNextSibling())
				{
					if ("value".equals(stringNode.getNodeName()))
					{
						bytes.add(Byte.decode(stringNode.getTextContent()));
					}
				}
				eventManager.getVariables().set(name, bytes);
				break;
			}
			case "Short":
			{
				final List<Short> shorts = new ArrayList<>();
				for (Node stringNode = variableNode.getFirstChild(); stringNode != null; stringNode = stringNode.getNextSibling())
				{
					if ("value".equals(stringNode.getNodeName()))
					{
						shorts.add(Short.decode(stringNode.getTextContent()));
					}
				}
				eventManager.getVariables().set(name, shorts);
				break;
			}
			case "Integer":
			{
				final List<Integer> integers = new ArrayList<>();
				for (Node stringNode = variableNode.getFirstChild(); stringNode != null; stringNode = stringNode.getNextSibling())
				{
					if ("value".equals(stringNode.getNodeName()))
					{
						integers.add(Integer.decode(stringNode.getTextContent()));
					}
				}
				eventManager.getVariables().set(name, integers);
				break;
			}
			case "Float":
			{
				final List<Float> floats = new ArrayList<>();
				for (Node stringNode = variableNode.getFirstChild(); stringNode != null; stringNode = stringNode.getNextSibling())
				{
					if ("value".equals(stringNode.getNodeName()))
					{
						floats.add(Float.parseFloat(stringNode.getTextContent()));
					}
				}
				eventManager.getVariables().set(name, floats);
				break;
			}
			case "Long":
			{
				final List<Long> longs = new ArrayList<>();
				for (Node stringNode = variableNode.getFirstChild(); stringNode != null; stringNode = stringNode.getNextSibling())
				{
					if ("value".equals(stringNode.getNodeName()))
					{
						longs.add(Long.decode(stringNode.getTextContent()));
					}
				}
				eventManager.getVariables().set(name, longs);
				break;
			}
			case "Double":
			{
				final List<Double> doubles = new ArrayList<>();
				for (Node stringNode = variableNode.getFirstChild(); stringNode != null; stringNode = stringNode.getNextSibling())
				{
					if ("value".equals(stringNode.getNodeName()))
					{
						doubles.add(Double.parseDouble(stringNode.getTextContent()));
					}
				}
				eventManager.getVariables().set(name, doubles);
				break;
			}
			case "String":
			{
				final List<String> strings = new ArrayList<>();
				for (Node stringNode = variableNode.getFirstChild(); stringNode != null; stringNode = stringNode.getNextSibling())
				{
					if ("value".equals(stringNode.getNodeName()))
					{
						strings.add(stringNode.getTextContent());
					}
				}
				eventManager.getVariables().set(name, strings);
				break;
			}
			case "ItemHolder":
			{
				final List<ItemHolder> items = new ArrayList<>();
				for (Node stringNode = variableNode.getFirstChild(); stringNode != null; stringNode = stringNode.getNextSibling())
				{
					if ("item".equals(stringNode.getNodeName()))
					{
						items.add(new ItemHolder(parseInteger(stringNode.getAttributes(), "id"), parseLong(stringNode.getAttributes(), "count")));
					}
				}
				eventManager.getVariables().set(name, items);
				break;
			}
			case "SkillHolder":
			{
				final List<SkillHolder> skils = new ArrayList<>();
				for (Node stringNode = variableNode.getFirstChild(); stringNode != null; stringNode = stringNode.getNextSibling())
				{
					if ("skill".equals(stringNode.getNodeName()))
					{
						skils.add(new SkillHolder(parseInteger(stringNode.getAttributes(), "id"), parseInteger(stringNode.getAttributes(), "level")));
					}
				}
				eventManager.getVariables().set(name, skils);
				break;
			}
			case "Location":
			{
				final List<Location> locations = new ArrayList<>();
				for (Node stringNode = variableNode.getFirstChild(); stringNode != null; stringNode = stringNode.getNextSibling())
				{
					if ("location".equals(stringNode.getNodeName()))
					{
						locations.add(new Location(parseInteger(stringNode.getAttributes(), "x"), parseInteger(stringNode.getAttributes(), "y"), parseInteger(stringNode.getAttributes(), "z", parseInteger(stringNode.getAttributes(), "heading", 0))));
					}
				}
				eventManager.getVariables().set(name, locations);
				break;
			}
			default:
			{
				LOGGER.info("Unhandled list case: {} for event: {}", type, eventManager.getClass().getSimpleName());
			}
		}
	}
	
	/**
	 * @param eventManager
	 * @param argsNode
	 * @return
	 */
	private Object parseArg(AbstractEventManager<?> eventManager, Node argsNode)
	{
		final String type = parseString(argsNode.getAttributes(), "type");
		switch (type)
		{
			case "Byte":
			{
				return Byte.decode(argsNode.getTextContent());
			}
			case "Short":
			{
				return Short.decode(argsNode.getTextContent());
			}
			case "Integer":
			{
				return Integer.decode(argsNode.getTextContent());
			}
			case "Float":
			{
				return Float.parseFloat(argsNode.getTextContent());
			}
			case "Long":
			{
				return Long.decode(argsNode.getTextContent());
			}
			case "Double":
			{
				return Double.parseDouble(argsNode.getTextContent());
			}
			case "String":
			{
				return argsNode.getTextContent();
			}
			default:
			{
				LOGGER.warn("Unhandled arg type: {} for event: {}", type, eventManager.getClass().getSimpleName());
				return null;
			}
		}
	}
	
	/**
	 * Gets the single instance of EventEngineData.
	 * @return single instance of EventEngineData
	 */
	public static EventEngineData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final EventEngineData _instance = new EventEngineData();
	}
}
