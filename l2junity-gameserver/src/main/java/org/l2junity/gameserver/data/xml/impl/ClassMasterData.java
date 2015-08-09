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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.l2junity.gameserver.data.xml.IGameXmlReader;
import org.l2junity.gameserver.enums.CategoryType;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.holders.ItemChanceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author Nik
 */
public final class ClassMasterData implements IGameXmlReader
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ClassMasterData.class);
	
	private boolean _isEnabled;
	private boolean _spawnClassMasters;
	private boolean _showEntireTree;
	private final List<ClassChangeData> _classChangeData = new LinkedList<>();
	private final List<Integer> _bannedClassIds = new LinkedList<>();
	
	/**
	 * Instantiates a new class list data.
	 */
	protected ClassMasterData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_classChangeData.clear();
		parseDatapackFile("config/ClassMaster.xml");
		LOGGER.info("Loaded {} Class change options.", _classChangeData.size());
	}
	
	@Override
	public boolean isValidating()
	{
		return false;
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		NamedNodeMap attrs;
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equals(n.getNodeName()))
			{
				for (Node cm = n.getFirstChild(); cm != null; cm = cm.getNextSibling())
				{
					attrs = cm.getAttributes();
					if ("classMaster".equals(cm.getNodeName()))
					{
						_isEnabled = parseBoolean(attrs, "classChangeEnabled", false);
						if (!_isEnabled)
						{
							return;
						}
						
						_spawnClassMasters = parseBoolean(attrs, "spawnClassMasters", true);
						_showEntireTree = parseBoolean(attrs, "showEntireTree", false);
						
						for (Node c = cm.getFirstChild(); c != null; c = c.getNextSibling())
						{
							attrs = c.getAttributes();
							if ("classChangeOption".equals(c.getNodeName()))
							{
								List<CategoryType> appliedCategories = new LinkedList<>();
								List<ItemChanceHolder> requiredItems = new LinkedList<>();
								List<ItemChanceHolder> rewardedItems = new LinkedList<>();
								boolean setNoble = false;
								boolean setHero = false;
								String optionName = parseString(attrs, "name", "");
								boolean showPopupWindow = parseBoolean(attrs, "showPopupWindow", false);
								for (Node b = c.getFirstChild(); b != null; b = b.getNextSibling())
								{
									attrs = b.getAttributes();
									if ("appliesTo".equals(b.getNodeName()))
									{
										for (Node r = b.getFirstChild(); r != null; r = r.getNextSibling())
										{
											attrs = r.getAttributes();
											if ("category".equals(r.getNodeName()))
											{
												CategoryType category = CategoryType.findByName(r.getTextContent().trim());
												if (category == null)
												{
													LOGGER.error("Incorrect category type: {}", r.getNodeValue());
													continue;
												}
												
												appliedCategories.add(category);
											}
										}
									}
									if ("rewards".equals(b.getNodeName()))
									{
										for (Node r = b.getFirstChild(); r != null; r = r.getNextSibling())
										{
											attrs = r.getAttributes();
											if ("item".equals(r.getNodeName()))
											{
												int itemId = parseInteger(attrs, "id");
												int count = parseInteger(attrs, "count", 1);
												int chance = parseInteger(attrs, "chance", 100);
												
												rewardedItems.add(new ItemChanceHolder(itemId, chance, count));
											}
											else if ("setNoble".equals(r.getNodeName()))
											{
												setNoble = true;
											}
											else if ("setHero".equals(r.getNodeName()))
											{
												setHero = true;
											}
										}
									}
									else if ("conditions".equals(b.getNodeName()))
									{
										for (Node r = b.getFirstChild(); r != null; r = r.getNextSibling())
										{
											attrs = r.getAttributes();
											if ("item".equals(r.getNodeName()))
											{
												int itemId = parseInteger(attrs, "id");
												int count = parseInteger(attrs, "count", 1);
												int chance = parseInteger(attrs, "chance", 100);
												
												requiredItems.add(new ItemChanceHolder(itemId, chance, count));
											}
										}
									}
								}
								
								if (appliedCategories.isEmpty())
								{
									LOGGER.warn("Class change option: {} has no categories to be applied on. Skipping!", optionName);
									continue;
								}
								
								ClassChangeData classChangeData = new ClassChangeData(optionName, appliedCategories, showPopupWindow);
								classChangeData.setItemsRequired(requiredItems);
								classChangeData.setItemsRewarded(rewardedItems);
								classChangeData.setRewardHero(setHero);
								classChangeData.setRewardNoblesse(setNoble);
								
								_classChangeData.add(classChangeData);
							}
							else if ("bannedClassIds".equals(c.getNodeName()))
							{
								for (Node b = c.getFirstChild(); b != null; b = b.getNextSibling())
								{
									if ("classId".equals(b.getNodeName()))
									{
										int classId = Integer.parseInt(b.getTextContent().trim());
										_bannedClassIds.add(classId);
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	public static class ClassChangeData
	{
		private final String _name;
		private final List<CategoryType> _appliedCategories;
		private final boolean _showPopupWindow;
		private boolean _rewardNoblesse;
		private boolean _rewardHero;
		private List<ItemChanceHolder> _itemsRequired;
		private List<ItemChanceHolder> _itemsRewarded;
		
		public ClassChangeData(String name, List<CategoryType> appliedCategories, boolean showPopupWindow)
		{
			_name = name;
			_appliedCategories = appliedCategories;
			_showPopupWindow = showPopupWindow;
		}
		
		public String getName()
		{
			return _name;
		}
		
		public boolean isShowPopupWindow()
		{
			return _showPopupWindow;
		}
		
		public List<CategoryType> getCategories()
		{
			return _appliedCategories != null ? _appliedCategories : Collections.emptyList();
		}
		
		public boolean isRewardNoblesse()
		{
			return _rewardNoblesse;
		}
		
		public void setRewardNoblesse(boolean rewardNoblesse)
		{
			_rewardNoblesse = rewardNoblesse;
		}
		
		public boolean isRewardHero()
		{
			return _rewardHero;
		}
		
		public void setRewardHero(boolean rewardHero)
		{
			_rewardHero = rewardHero;
		}
		
		void setItemsRequired(List<ItemChanceHolder> itemsRequired)
		{
			_itemsRequired = itemsRequired;
		}
		
		public List<ItemChanceHolder> getItemsRequired()
		{
			return _itemsRequired != null ? _itemsRequired : Collections.emptyList();
		}
		
		void setItemsRewarded(List<ItemChanceHolder> itemsRewarded)
		{
			_itemsRewarded = itemsRewarded;
		}
		
		public List<ItemChanceHolder> getItemsRewarded()
		{
			return _itemsRewarded != null ? _itemsRewarded : Collections.emptyList();
		}
		
	}
	
	public boolean isEnabled()
	{
		return _isEnabled;
	}
	
	public boolean isSpawnClassMasters()
	{
		return _spawnClassMasters;
	}
	
	public boolean isShowEntireTree()
	{
		return _showEntireTree;
	}
	
	public List<ClassChangeData> getClassChangeData()
	{
		return _classChangeData;
	}
	
	public boolean isClassChangeAvailableShowPopup(PlayerInstance player)
	{
		return getClassChangeData().stream().filter(ClassChangeData::isShowPopupWindow).flatMap(ccd -> ccd.getCategories().stream()).anyMatch(ct -> player.isInCategory(ct));
	}
	
	public boolean isClassChangeAvailable(PlayerInstance player)
	{
		return getClassChangeData().stream().flatMap(ccd -> ccd.getCategories().stream()).anyMatch(ct -> player.isInCategory(ct));
	}
	
	public ClassChangeData getClassChangeData(int index)
	{
		if ((index >= 0) && (index < _classChangeData.size()))
		{
			return _classChangeData.get(index);
		}
		
		return null;
	}
	
	/**
	 * Gets the single instance of ClassMasterData.
	 * @return single instance of ClassMasterData
	 */
	public static ClassMasterData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ClassMasterData _instance = new ClassMasterData();
	}
}
