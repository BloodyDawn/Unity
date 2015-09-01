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
import java.util.Map;

import org.l2junity.commons.util.IXmlReader;
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
			else if ("map".equals(variableNode.getNodeName()))
			{
				parseMapVariables(eventManager, variableNode);
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
								final String type = parseString(argsNode.getAttributes(), "type");
								final Object value = parseObject(eventManager, type, argsNode.getTextContent());
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
	@SuppressWarnings("unchecked")
	private void parseListVariables(AbstractEventManager<?> eventManager, Node variableNode)
	{
		final String name = parseString(variableNode.getAttributes(), "name");
		final String type = parseString(variableNode.getAttributes(), "type");
		final Class<?> classType = getClassByName(eventManager, type);
		final List<?> values = newList(classType);
		switch (type)
		{
			case "Byte":
			case "Short":
			case "Integer":
			case "Float":
			case "Long":
			case "Double":
			case "String":
			{
				for (Node stringNode = variableNode.getFirstChild(); stringNode != null; stringNode = stringNode.getNextSibling())
				{
					if ("value".equals(stringNode.getNodeName()))
					{
						((List<Object>) values).add(parseObject(eventManager, type, stringNode.getTextContent()));
					}
				}
				break;
			}
			case "ItemHolder":
			{
				for (Node stringNode = variableNode.getFirstChild(); stringNode != null; stringNode = stringNode.getNextSibling())
				{
					if ("item".equals(stringNode.getNodeName()))
					{
						((List<Object>) values).add(new ItemHolder(parseInteger(stringNode.getAttributes(), "id"), parseLong(stringNode.getAttributes(), "count")));
					}
				}
				break;
			}
			case "SkillHolder":
			{
				for (Node stringNode = variableNode.getFirstChild(); stringNode != null; stringNode = stringNode.getNextSibling())
				{
					if ("skill".equals(stringNode.getNodeName()))
					{
						((List<Object>) values).add(new SkillHolder(parseInteger(stringNode.getAttributes(), "id"), parseInteger(stringNode.getAttributes(), "level")));
					}
				}
				break;
			}
			case "Location":
			{
				for (Node stringNode = variableNode.getFirstChild(); stringNode != null; stringNode = stringNode.getNextSibling())
				{
					if ("location".equals(stringNode.getNodeName()))
					{
						((List<Object>) values).add(new Location(parseInteger(stringNode.getAttributes(), "x"), parseInteger(stringNode.getAttributes(), "y"), parseInteger(stringNode.getAttributes(), "z", parseInteger(stringNode.getAttributes(), "heading", 0))));
					}
				}
				break;
			}
			default:
			{
				LOGGER.info("Unhandled list case: {} for event: {}", type, eventManager.getClass().getSimpleName());
				break;
			}
		}
		eventManager.getVariables().set(name, values);
	}
	
	/**
	 * @param eventManager
	 * @param variableNode
	 */
	@SuppressWarnings("unchecked")
	private void parseMapVariables(AbstractEventManager<?> eventManager, Node variableNode)
	{
		final String name = parseString(variableNode.getAttributes(), "name");
		final String keyType = parseString(variableNode.getAttributes(), "keyType");
		final String valueType = parseString(variableNode.getAttributes(), "valueType");
		final Class<?> keyClass = getClassByName(eventManager, keyType);
		final Class<?> valueClass = getClassByName(eventManager, valueType);
		final Map<?, ?> map = newMap(keyClass, valueClass);
		forEach(variableNode, IXmlReader::isNode, stringNode ->
		{
			switch (stringNode.getNodeName())
			{
				case "entry":
				{
					final NamedNodeMap attrs = stringNode.getAttributes();
					((Map<Object, Object>) map).put(parseObject(eventManager, keyType, parseString(attrs, "key")), parseObject(eventManager, valueType, parseString(attrs, "value")));
					break;
				}
				case "item":
				{
					final NamedNodeMap attrs = stringNode.getAttributes();
					((Map<Object, ItemHolder>) map).put(parseObject(eventManager, keyType, parseString(attrs, "key")), new ItemHolder(parseInteger(stringNode.getAttributes(), "id"), parseLong(stringNode.getAttributes(), "count")));
					break;
				}
				case "skill":
				{
					final NamedNodeMap attrs = stringNode.getAttributes();
					((Map<Object, SkillHolder>) map).put(parseObject(eventManager, keyType, parseString(attrs, "key")), new SkillHolder(parseInteger(stringNode.getAttributes(), "id"), parseInteger(stringNode.getAttributes(), "level")));
					break;
				}
				case "location":
				{
					final NamedNodeMap attrs = stringNode.getAttributes();
					((Map<Object, Location>) map).put(parseObject(eventManager, keyType, parseString(attrs, "key")), new Location(parseInteger(stringNode.getAttributes(), "x"), parseInteger(stringNode.getAttributes(), "y"), parseInteger(stringNode.getAttributes(), "z", parseInteger(stringNode.getAttributes(), "heading", 0))));
					break;
				}
				default:
				{
					LOGGER.info("Unhandled map case: {} {} for event: {}", name, stringNode.getNodeName(), eventManager.getClass().getSimpleName());
				}
			}
		});
		eventManager.getVariables().set(name, map);
	}
	
	private Class<?> getClassByName(AbstractEventManager<?> eventManager, String name)
	{
		switch (name)
		{
			case "Byte":
				return Byte.class;
			case "Short":
				return Short.class;
			case "Integer":
				return Integer.class;
			case "Float":
				return Float.class;
			case "Long":
				return Long.class;
			case "Double":
				return Double.class;
			case "String":
				return String.class;
			case "ItemHolder":
				return ItemHolder.class;
			case "SkillHolder":
				return SkillHolder.class;
			case "Location":
				return Location.class;
			default:
				LOGGER.info("Unhandled class case: {} for event: {}", name, eventManager.getClass().getSimpleName());
				return Object.class;
		}
	}
	
	private Object parseObject(AbstractEventManager<?> eventManager, String type, String value)
	{
		switch (type)
		{
			case "Byte":
			{
				return Byte.decode(value);
			}
			case "Short":
			{
				return Short.decode(value);
			}
			case "Integer":
			{
				return Integer.decode(value);
			}
			case "Float":
			{
				return Float.parseFloat(value);
			}
			case "Long":
			{
				return Long.parseLong(value);
			}
			case "Double":
			{
				return Double.parseDouble(value);
			}
			case "String":
			{
				return value;
			}
			default:
			{
				LOGGER.info("Unhandled object case: {} for event: {}", type, eventManager.getClass().getSimpleName());
				return null;
			}
		}
	}
	
	private static <T> List<T> newList(Class<T> type)
	{
		return new ArrayList<>();
	}
	
	private static <K, V> Map<K, V> newMap(Class<K> keyClass, Class<V> valueClass)
	{
		return new LinkedHashMap<>();
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
