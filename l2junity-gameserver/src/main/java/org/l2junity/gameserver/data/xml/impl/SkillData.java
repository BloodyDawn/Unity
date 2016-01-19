package org.l2junity.gameserver.data.xml.impl;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.l2junity.gameserver.data.xml.IGameXmlReader;
import org.l2junity.gameserver.model.StatsSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import net.objecthunter.exp4j.ExpressionBuilder;

/**
 * Skill data parser.
 * @author NosBit
 */
public class SkillData implements IGameXmlReader
{
	private DocumentBuilder documentBuilder;
	
	private static final Set<String> BLOCK_ITEM_VALUE_ELEMENTS = new HashSet<>();
	private static final Set<String> BLOCK_ITEM_ELEMENTS = new HashSet<>();
	
	static
	{
		BLOCK_ITEM_VALUE_ELEMENTS.add("item");
		BLOCK_ITEM_VALUE_ELEMENTS.add("value");
		BLOCK_ITEM_ELEMENTS.add("item");
	}
	
	protected SkillData()
	{
		try
		{
			documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		}
		catch (ParserConfigurationException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean isValidating()
	{
		return false;
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("l2junity-gameserver/dist/game/data/stats/skills/test.xml");
		// long t = System.nanoTime();
		// parseDatapackDirectory("l2junity-gameserver/dist/game/data/stats/skills/", false);
		// System.out.println(System.nanoTime() - t);
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		for (Node node = doc.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if ("list".equalsIgnoreCase(node.getNodeName()))
			{
				for (Node listNode = node.getFirstChild(); listNode != null; listNode = listNode.getNextSibling())
				{
					if ("skill".equalsIgnoreCase(listNode.getNodeName()))
					{
						NamedNodeMap attributes = listNode.getAttributes();
						
						final Map<Integer, Map<Integer, StatsSet>> skillInfo = new HashMap<>();
						final StatsSet generalSkillInfo = new StatsSet();
						
						final Map<String, Map<Integer, Map<Integer, Object>>> variableValues = new HashMap<>();
						final Map<String, Object> variableGeneralValues = new HashMap<>();
						
						for (Node skillNode = listNode.getFirstChild(); skillNode != null; skillNode = skillNode.getNextSibling())
						{
							final String skillNodeName = skillNode.getNodeName();
							switch (skillNodeName.toLowerCase())
							{
								case "variable":
								{
									attributes = skillNode.getAttributes();
									final String name = "@" + parseString(attributes, "name");
									variableGeneralValues.put(name, parseValues(skillNode, variableValues.computeIfAbsent(name, k -> new HashMap<>())));
									break;
								}
								case "effects":
								case "pveeffects":
								case "pvpeffects":
								case "starteffects":
								case "endeffects":
								{
									for (Node effectsNode = skillNode.getFirstChild(); effectsNode != null; effectsNode = effectsNode.getNextSibling())
									{
										switch (effectsNode.getNodeName().toLowerCase())
										{
											case "effect":
											{
												final Map<Integer, Map<Integer, StatsSet>> effectInfo = new HashMap<>();
												final StatsSet generalEffectInfo = new StatsSet();
												for (Node effectNode = effectsNode.getFirstChild(); effectNode != null; effectNode = effectNode.getNextSibling())
												{
													parseInfo(effectNode, variableValues, variableGeneralValues, effectInfo, generalEffectInfo);
												}
												
												for (Entry<String, Object> stat : generalEffectInfo.getSet().entrySet())
												{
													effectInfo.forEach((level, subLevelMap) ->
													{
														subLevelMap.forEach((subLevel, statsSet) ->
														{
															statsSet.getSet().putIfAbsent(stat.getKey(), stat.getValue());
														});
													});
												}
												System.out.println(effectInfo.toString().replace(",", "\n"));
												break;
											}
										}
									}
									break;
								}
								default:
								{
									parseInfo(skillNode, variableValues, variableGeneralValues, skillInfo, generalSkillInfo);
									break;
								}
							}
						}
						
						for (Entry<String, Object> stat : generalSkillInfo.getSet().entrySet())
						{
							skillInfo.forEach((level, subLevelMap) ->
							{
								subLevelMap.forEach((subLevel, statsSet) ->
								{
									statsSet.getSet().putIfAbsent(stat.getKey(), stat.getValue());
								});
							});
						}
						System.out.println(skillInfo.toString().replace(",", "\n"));
					}
				}
			}
		}
	}
	
	private void parseInfo(Node node, Map<String, Map<Integer, Map<Integer, Object>>> variableValues, Map<String, Object> variableGeneralValues, Map<Integer, Map<Integer, StatsSet>> info, StatsSet generalInfo)
	{
		Map<Integer, Map<Integer, Object>> values = new HashMap<>();
		Object generalValue = parseValues(node, values);
		if (generalValue != null)
		{
			String stringGeneralValue = String.valueOf(generalValue);
			if (stringGeneralValue.startsWith("@"))
			{
				Map<Integer, Map<Integer, Object>> tableValue = variableValues.get(stringGeneralValue);
				if (tableValue != null)
				{
					if (!tableValue.isEmpty())
					{
						values = tableValue;
					}
					else
					{
						generalInfo.set(node.getNodeName(), variableGeneralValues.get(stringGeneralValue));
					}
				}
				else
				{
					throw new IllegalArgumentException("undefined variable " + stringGeneralValue);
				}
			}
			else
			{
				generalInfo.set(node.getNodeName(), generalValue);
			}
		}
		
		values.forEach((level, subLevelMap) ->
		{
			subLevelMap.forEach((subLevel, value) ->
			{
				info.computeIfAbsent(level, k -> new HashMap<>()).computeIfAbsent(subLevel, k -> new StatsSet()).set(node.getNodeName(), value);
			});
		});
	}
	
	private Object parseValues(Node node, Map<Integer, Map<Integer, Object>> values)
	{
		Object parsedValue = parseValue(node, BLOCK_ITEM_VALUE_ELEMENTS);
		if (parsedValue != null)
		{
			return parsedValue;
		}
		
		List<Object> list = null;
		for (node = node.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if (node.getNodeName().equalsIgnoreCase("item"))
			{
				if (list == null)
				{
					list = new LinkedList<>();
				}
				
				parsedValue = parseValue(node);
				if (parsedValue != null)
				{
					list.add(parsedValue);
				}
			}
			else if (node.getNodeName().equalsIgnoreCase("value"))
			{
				final NamedNodeMap attributes = node.getAttributes();
				final Integer level = parseInteger(attributes, "level");
				if (level != null)
				{
					parsedValue = parseValue(node);
					if (parsedValue != null)
					{
						final Integer subLevel = parseInteger(attributes, "subLevel", 0);
						values.computeIfAbsent(level, k -> new HashMap<>()).put(subLevel, parsedValue);
					}
				}
				else
				{
					final int fromLevel = parseInteger(attributes, "fromLevel");
					final int toLevel = parseInteger(attributes, "toLevel");
					final int fromSubLevel = parseInteger(attributes, "fromSubLevel", 0);
					final int toSubLevel = parseInteger(attributes, "toSubLevel", 0);
					for (int i = fromLevel; i <= toLevel; i++)
					{
						for (int j = fromSubLevel; j <= toSubLevel; j++)
						{
							Map<Integer, Object> subValues = values.computeIfAbsent(i, k -> new HashMap<>());
							Map<String, Double> variables = new HashMap<>();
							variables.put("index", (i - fromLevel) + 1d);
							variables.put("subIndex", (j - fromSubLevel) + 1d);
							Object base = values.getOrDefault(i, Collections.emptyMap()).get(0);
							if ((base != null) && !(base instanceof StatsSet))
							{
								variables.put("base", Double.parseDouble(String.valueOf(base)));
							}
							parsedValue = parseValue(node, BLOCK_ITEM_ELEMENTS, variables);
							if (parsedValue != null)
							{
								subValues.put(j, parsedValue);
							}
							else
							{
								variables.remove("base");
								List<Object> list2 = null;
								for (Node valueNode = node.getFirstChild(); valueNode != null; valueNode = valueNode.getNextSibling())
								{
									if (valueNode.getNodeName().equalsIgnoreCase("item"))
									{
										if (list2 == null)
										{
											list2 = new LinkedList<>();
										}
										
										parsedValue = parseValue(valueNode, Collections.emptySet(), variables);
										if (parsedValue != null)
										{
											list2.add(parsedValue);
										}
									}
								}
								if (list2 != null)
								{
									subValues.put(j, list2);
								}
							}
						}
					}
				}
			}
		}
		return list;
	}
	
	private Object parseValue(Node node, Set<String> blockedNodeNames, Map<String, Double> variables)
	{
		StatsSet statsSet = null;
		for (node = node.getFirstChild(); node != null; node = node.getNextSibling())
		{
			final String nodeName = node.getNodeName();
			if (nodeName.equalsIgnoreCase("#text"))
			{
				final String value = node.getNodeValue().trim();
				if (!value.isEmpty())
				{
					if (value.startsWith("{") && value.endsWith("}"))
					{
						return new ExpressionBuilder(value).variables(variables.keySet()).build().setVariables(variables).evaluate();
					}
					return value;
				}
			}
			else if (!blockedNodeNames.contains(nodeName.toLowerCase()))
			{
				final String value = node.getTextContent().trim();
				if (!value.isEmpty())
				{
					if (statsSet == null)
					{
						statsSet = new StatsSet();
					}
					
					if (value.startsWith("{") && value.endsWith("}"))
					{
						statsSet.set(nodeName, new ExpressionBuilder(value).variables(variables.keySet()).build().setVariables(variables).evaluate());
					}
					else
					{
						statsSet.set(nodeName, value);
					}
					
					final NamedNodeMap attributes = node.getAttributes();
					for (int i = 0; i < attributes.getLength(); i++)
					{
						final Node attributeNode = attributes.item(i);
						final String attributeValue = attributeNode.getNodeValue();
						if (attributeValue.startsWith("{") && attributeValue.endsWith("}"))
						{
							statsSet.set(nodeName + "." + attributeNode.getNodeName(), new ExpressionBuilder(attributeValue).variables(variables.keySet()).build().setVariables(variables).evaluate());
						}
						else
						{
							statsSet.set(nodeName + "." + attributeNode.getNodeName(), attributeValue);
						}
					}
				}
			}
		}
		return statsSet;
	}
	
	private Object parseValue(Node value, Set<String> blockedNodeNames)
	{
		return parseValue(value, blockedNodeNames, Collections.emptyMap());
	}
	
	private Object parseValue(Node value)
	{
		return parseValue(value, Collections.emptySet());
	}
	
	public static void main(String[] args)
	{
		new SkillData().load();
	}
}
