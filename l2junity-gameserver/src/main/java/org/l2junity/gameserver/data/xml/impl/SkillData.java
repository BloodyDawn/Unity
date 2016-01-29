package org.l2junity.gameserver.data.xml.impl;

import net.objecthunter.exp4j.ExpressionBuilder;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.l2junity.Config;
import org.l2junity.gameserver.data.xml.IGameXmlReader;
import org.l2junity.gameserver.handler.EffectHandler;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.skills.CommonSkill;
import org.l2junity.gameserver.model.skills.EffectScope;
import org.l2junity.gameserver.model.skills.Skill;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Skill data parser.
 * @author NosBit
 */
public class SkillData implements IGameXmlReader
{
	private static final Set<String> BLOCK_ITEM_VALUE_ELEMENTS = new HashSet<>();
	private static final Set<String> BLOCK_ITEM_ELEMENTS = new HashSet<>();

	private final Map<Integer, Skill> _skills = new HashMap<>();
	private final Map<Integer, Integer> _skillsMaxLevel = new HashMap<>();
	private final Set<Integer> _enchantable = new HashSet<>();

	static
	{
		BLOCK_ITEM_VALUE_ELEMENTS.add("item");
		BLOCK_ITEM_VALUE_ELEMENTS.add("value");
		BLOCK_ITEM_ELEMENTS.add("item");
	}
	
	private class NamedParamInfo
	{
		private final String name;
		private final Integer fromLevel;
		private final Integer toLevel;
		private final Integer fromSubLevel;
		private final Integer toSubLevel;
		private final Map<Integer, Map<Integer, StatsSet>> info;
		private final StatsSet generalInfo;
		
		public NamedParamInfo(String name, Integer fromLevel, Integer toLevel, Integer fromSubLevel, Integer toSubLevel, Map<Integer, Map<Integer, StatsSet>> info, StatsSet generalInfo)
		{
			this.name = name;
			this.fromLevel = fromLevel;
			this.toLevel = toLevel;
			this.fromSubLevel = fromSubLevel;
			this.toSubLevel = toSubLevel;
			this.info = info;
			this.generalInfo = generalInfo;
		}
	}
	
	protected SkillData()
	{
		load();
	}

	/**
	 * Provides the skill hash
	 * @param skill The L2Skill to be hashed
	 * @return getSkillHashCode(skill.getId(), skill.getLevel())
	 */
	public static int getSkillHashCode(Skill skill)
	{
		return getSkillHashCode(skill.getId(), skill.getLevel());
	}

	/**
	 * Centralized method for easier change of the hashing sys
	 * @param skillId The Skill Id
	 * @param skillLevel The Skill Level
	 * @return The Skill hash number
	 */
	public static int getSkillHashCode(int skillId, int skillLevel)
	{
		return (skillId * 1021) + skillLevel;
	}

	public Skill getSkill(int skillId, int level)
	{
		final Skill result = _skills.get(getSkillHashCode(skillId, level));
		if (result != null)
		{
			return result;
		}

		// skill/level not found, fix for transformation scripts
		final int maxLvl = getMaxLevel(skillId);
		// requested level too high
		if ((maxLvl > 0) && (level > maxLvl))
		{
			if (Config.DEBUG)
			{
				LOGGER.warn("call to unexisting skill level id: {} requested level: {} max level: {}", skillId, level, maxLvl, new Throwable());
			}
			return _skills.get(getSkillHashCode(skillId, maxLvl));
		}

		LOGGER.warn("No skill info found for skill id {} and skill level {}", skillId, level);
		return null;
	}

	public int getMaxLevel(int skillId)
	{
		final Integer maxLevel = _skillsMaxLevel.get(skillId);
		return maxLevel != null ? maxLevel : 0;
	}

	/**
	 * Verifies if the given skill ID correspond to an enchantable skill.
	 * @param skillId the skill ID
	 * @return {@code true} if the skill is enchantable, {@code false} otherwise
	 */
	public boolean isEnchantable(int skillId)
	{
		return _enchantable.contains(skillId);
	}

	/**
	 * @param addNoble
	 * @param hasCastle
	 * @return an array with siege skills. If addNoble == true, will add also Advanced headquarters.
	 */
	public List<Skill> getSiegeSkills(boolean addNoble, boolean hasCastle)
	{
		final List<Skill> temp = new LinkedList<>();

		temp.add(_skills.get(SkillData.getSkillHashCode(CommonSkill.IMPRIT_OF_LIGHT.getId(), 1)));
		temp.add(_skills.get(SkillData.getSkillHashCode(CommonSkill.IMPRIT_OF_DARKNESS.getId(), 1)));

		temp.add(_skills.get(SkillData.getSkillHashCode(247, 1))); // Build Headquarters

		if (addNoble)
		{
			temp.add(_skills.get(SkillData.getSkillHashCode(326, 1))); // Build Advanced Headquarters
		}
		if (hasCastle)
		{
			temp.add(_skills.get(SkillData.getSkillHashCode(844, 1))); // Outpost Construction
			temp.add(_skills.get(SkillData.getSkillHashCode(845, 1))); // Outpost Demolition
		}
		return temp;
	}

	@Override
	public boolean isValidating()
	{
		return false;
	}

	@Override
	public synchronized void load()
	{
		_skills.clear();
		_skillsMaxLevel.clear();
		_enchantable.clear();
		parseDatapackDirectory("data/stats/skills/", true);
	}

	public void reload()
	{
		load();
		// Reload Skill Tree as well.
		SkillTreesData.getInstance().load();
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
						final Map<Integer, Set<Integer>> levels = new HashMap<>();
						final Map<Integer, Map<Integer, StatsSet>> skillInfo = new HashMap<>();
						final StatsSet generalSkillInfo = new StatsSet();
						
						for (int i = 0; i < attributes.getLength(); i++)
						{
							final Node attributeNode = attributes.item(i);
							final String attributeValue = attributeNode.getNodeValue();
							if (attributeValue.startsWith("{") && attributeValue.endsWith("}"))
							{
								generalSkillInfo.set("skill." + attributeNode.getNodeName(), new ExpressionBuilder(attributeValue).build().evaluate());
							}
							else
							{
								generalSkillInfo.set("skill." + attributeNode.getNodeName(), attributeValue);
							}
						}
						
						final Map<String, Map<Integer, Map<Integer, Object>>> variableValues = new HashMap<>();
						final Map<String, Object> variableGeneralValues = new HashMap<>();
						final Map<EffectScope, List<NamedParamInfo>> effectParamInfo = new HashMap<>();
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
								default:
								{
									final EffectScope effectScope = EffectScope.findByXmlNodeName(skillNodeName);
									if (effectScope != null)
									{
										for (Node effectsNode = skillNode.getFirstChild(); effectsNode != null; effectsNode = effectsNode.getNextSibling())
										{
											switch (effectsNode.getNodeName().toLowerCase())
											{
												case "effect":
												{
													attributes = effectsNode.getAttributes();
													final String name = parseString(attributes, "name");
													final Integer level = parseInteger(attributes, "level");
													final Integer fromLevel = parseInteger(attributes, "fromLevel", level);
													final Integer toLevel = parseInteger(attributes, "toLevel", level);
													final Integer subLevel = parseInteger(attributes, "subLevel", 0);
													final Integer fromSubLevel = parseInteger(attributes, "fromSubLevel", subLevel);
													final Integer toSubLevel = parseInteger(attributes, "toSubLevel", subLevel);
													final Map<Integer, Map<Integer, StatsSet>> effectInfo = new HashMap<>();
													final StatsSet generalEffectInfo = new StatsSet();
													for (Node effectNode = effectsNode.getFirstChild(); effectNode != null; effectNode = effectNode.getNextSibling())
													{
														parseInfo(effectNode, variableValues, variableGeneralValues, effectInfo, generalEffectInfo);
													}
													effectParamInfo.computeIfAbsent(effectScope, k -> new LinkedList<>()).add(new NamedParamInfo(name, fromLevel, toLevel, fromSubLevel, toSubLevel, effectInfo, generalEffectInfo));
													break;
												}
											}
										}
										break;
									}
									else
									{
										parseInfo(skillNode, variableValues, variableGeneralValues, skillInfo, generalSkillInfo);
									}
									break;
								}
							}
						}
						
						final int skillLevels = generalSkillInfo.getInt("skill.levels", 0);
						for (int i = 1; i <= skillLevels; i++)
						{
							levels.computeIfAbsent(i, k -> new HashSet<>()).add(0);
						}
						
						skillInfo.forEach((level, subLevelMap) -> {
							subLevelMap.forEach((subLevel, statsSet) -> {
								levels.computeIfAbsent(level, k -> new HashSet<>()).add(subLevel);
							});
						});
						
						effectParamInfo.forEach(((effectScope, namedParamInfos) -> {
							namedParamInfos.forEach(namedParamInfo -> {
								namedParamInfo.info.forEach((level, subLevelMap) -> {
									subLevelMap.forEach((subLevel, statsSet) -> {
										levels.computeIfAbsent(level, k -> new HashSet<>()).add(subLevel);
									});
								});
								
								if ((namedParamInfo.fromLevel != null) && (namedParamInfo.toLevel != null))
								{
									
									for (int i = namedParamInfo.fromLevel; i <= namedParamInfo.toLevel; i++)
									{
										if ((namedParamInfo.fromSubLevel != null) && (namedParamInfo.toSubLevel != null))
										{
											for (int j = namedParamInfo.fromSubLevel; j <= namedParamInfo.toSubLevel; j++)
											{
												
												levels.computeIfAbsent(i, k -> new HashSet<>()).add(j);
											}
										}
										else
										{
											levels.computeIfAbsent(i, k -> new HashSet<>()).add(0);
										}
									}
								}
							});
						}));
						
						levels.forEach((level, subLevels) -> {
							subLevels.forEach(subLevel -> {
								final StatsSet statsSet = Optional.ofNullable(skillInfo.getOrDefault(level, Collections.emptyMap()).get(subLevel)).orElseGet(() -> new StatsSet());
								generalSkillInfo.getSet().forEach((k, v) -> statsSet.getSet().putIfAbsent(k, v));
								statsSet.set("skill.level", level);
								statsSet.set("skill.subLevel", subLevel);
								final Skill skill = new Skill(statsSet);
								effectParamInfo.forEach((effectScope, namedParamInfos) -> {
									namedParamInfos.forEach(namedParamInfo -> {
										if (((namedParamInfo.fromLevel == null) && (namedParamInfo.toLevel == null)) || ((namedParamInfo.fromLevel >= level) && (namedParamInfo.toLevel <= level)))
										{
											if (((namedParamInfo.fromSubLevel == null) && (namedParamInfo.toSubLevel == null)) || ((namedParamInfo.fromSubLevel >= subLevel) && (namedParamInfo.toSubLevel <= subLevel)))
											{
												final StatsSet params = Optional.ofNullable(namedParamInfo.info.getOrDefault(level, Collections.emptyMap()).get(subLevel)).orElseGet(() -> new StatsSet());
												namedParamInfo.generalInfo.getSet().forEach((k, v) -> params.getSet().putIfAbsent(k, v));
												
												try
												{
													skill.addEffect(effectScope, EffectHandler.getInstance().getHandlerFactory(namedParamInfo.name).apply(params));
												}
												catch (Exception e)
												{
													LOGGER.warn("Failed loading effect for Skill Id[{}] Level[{}] SubLevel[{}] Effect Scope[{}] Effect Name[{}]", statsSet.getInt("skill.id"), level, subLevel, effectScope, namedParamInfo.name, e);
												}
											}
										}
									});
								});
								_skills.put(getSkillHashCode(skill), skill);
								_skillsMaxLevel.merge(skill.getId(), skill.getLevel(), Integer::max);
								//TODO: add enchantable
							});
						});
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
		
		values.forEach((level, subLevelMap) -> {
			subLevelMap.forEach((subLevel, value) -> {
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
	
	public static SkillData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final SkillData _instance = new SkillData();
	}
}
