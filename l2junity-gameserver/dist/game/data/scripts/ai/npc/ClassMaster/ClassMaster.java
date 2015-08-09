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
package ai.npc.ClassMaster;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.l2junity.Config;
import org.l2junity.commons.util.CommonUtil;
import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.data.xml.IGameXmlReader;
import org.l2junity.gameserver.data.xml.impl.ClassListData;
import org.l2junity.gameserver.datatables.ItemTable;
import org.l2junity.gameserver.enums.CategoryType;
import org.l2junity.gameserver.enums.HtmlActionScope;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.base.ClassId;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.ListenerRegisterType;
import org.l2junity.gameserver.model.events.annotations.RegisterEvent;
import org.l2junity.gameserver.model.events.annotations.RegisterType;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerBypass;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerLevelChanged;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerLogin;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerPressTutorialMark;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerProfessionChange;
import org.l2junity.gameserver.model.holders.ItemChanceHolder;
import org.l2junity.gameserver.model.holders.ItemHolder;
import org.l2junity.gameserver.model.spawns.NpcSpawnTemplate;
import org.l2junity.gameserver.model.spawns.SpawnGroup;
import org.l2junity.gameserver.model.spawns.SpawnTemplate;
import org.l2junity.gameserver.network.client.send.TutorialCloseHtml;
import org.l2junity.gameserver.network.client.send.TutorialShowHtml;
import org.l2junity.gameserver.network.client.send.TutorialShowQuestionMark;
import org.l2junity.gameserver.network.client.send.UserInfo;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ai.npc.AbstractNpcAI;

/**
 * @author Nik
 */
public class ClassMaster extends AbstractNpcAI implements IGameXmlReader
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ClassMaster.class);
	
	private boolean _isEnabled;
	private boolean _spawnClassMasters;
	private boolean _showEntireTree;
	private final List<ClassChangeData> _classChangeData = new LinkedList<>();
	private final List<NpcSpawnTemplate> _spawnTemplates = new LinkedList<>();
	private final List<Integer> _bannedClassIds = new LinkedList<>();
	
	// Npc
	private static final int[] CLASS_MASTER =
	{
		31756, // Mulia
		31757, // Ilia
	};
	
	public ClassMaster()
	{
		super(ClassMaster.class.getSimpleName(), "ai/npc");
		load();
		addStartNpc(CLASS_MASTER);
		addTalkId(CLASS_MASTER);
		addFirstTalkId(CLASS_MASTER);
		
		// Spawn NPCs
		if (isSpawnClassMasters())
		{
			_spawnTemplates.forEach(NpcSpawnTemplate::spawn);
		}
	}
	
	@Override
	public void load()
	{
		_classChangeData.clear();
		_spawnTemplates.clear();
		_bannedClassIds.clear();
		parseDatapackFile("config/ClassMaster.xml");
		
		LOGGER.info("Loaded {} class change options.", _classChangeData.size());
		LOGGER.info("Loaded {} class master spawns.", _spawnTemplates.size());
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
					else if ("spawnlist".equals(cm.getNodeName()))
					{
						SpawnGroup group = new SpawnGroup(StatsSet.EMPTY_STATSET);
						final SpawnTemplate spawnTemplate = new SpawnTemplate(StatsSet.EMPTY_STATSET, f);
						for (Node d = cm.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("npc".equalsIgnoreCase(d.getNodeName()))
							{
								final StatsSet set = new StatsSet();
								attrs = d.getAttributes();
								for (int i = 0; i < attrs.getLength(); i++)
								{
									final Node node = attrs.item(i);
									set.set(node.getNodeName(), node.getNodeValue());
								}
								
								try
								{
									final NpcSpawnTemplate npcTemplate = new NpcSpawnTemplate(spawnTemplate, group, set);
									if (CommonUtil.contains(CLASS_MASTER, npcTemplate.getId()))
									{
										_spawnTemplates.add(npcTemplate);
									}
									else
									{
										LOGGER.warn("NPC ID: {} defined in {} spawnlist is not part of the ClassMaster script.", npcTemplate.getId(), f.getName());
									}
								}
								catch (Exception e)
								{
									LOGGER.warn("Error while spawning class master npc: ", e);
								}
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		String htmltext = event;
		int classLevelMenu = 0;
		StringTokenizer st = new StringTokenizer(event);
		event = st.nextToken();
		
		if (event.startsWith("1stClass"))
		{
			classLevelMenu = 1;
		}
		else if (event.startsWith("2ndClass"))
		{
			classLevelMenu = 2;
		}
		else if (event.startsWith("3rdClass"))
		{
			classLevelMenu = 3;
		}
		else if (event.startsWith("awaken"))
		{
			classLevelMenu = 4;
		}
		else if (event.startsWith("change_class") && st.hasMoreTokens())
		{
			int classId = Integer.parseInt(st.nextToken());
			if (st.hasMoreTokens())
			{
				int classDataIndex = Integer.parseInt(st.nextToken());
				if (checkAndChangeClass(player, classId, classDataIndex))
				{
					htmltext = getHtm(player.getHtmlPrefix(), "ok.html");
					htmltext = htmltext.replace("%name%", ClassListData.getInstance().getClass(classId).getClientCode());
				}
			}
			else
			{
				htmltext = getHtm(player.getHtmlPrefix(), "templateOptions.html");
				htmltext = htmltext.replace("%name%", ClassListData.getInstance().getClass(classId).getClientCode());
				htmltext = htmltext.replace("%options%", getClassChangeOptions(player, classId, false));
			}
			
		}
		else if (event.startsWith("become_noble"))
		{
			if (!player.isNoble())
			{
				player.setNoble(true);
				player.sendPacket(new UserInfo(player));
				htmltext = getHtm(player.getHtmlPrefix(), "nobleok.html");
			}
			else
			{
				return null;
			}
		}
		else if (event.startsWith("learn_skills"))
		{
			player.giveAvailableSkills(Config.AUTO_LEARN_FS_SKILLS, true);
			return null;
		}
		else if (event.startsWith("increase_clan_level"))
		{
			if (!player.isClanLeader())
			{
				htmltext = getHtm(player.getHtmlPrefix(), "noclanleader.html");
			}
			else if (player.getClan().getLevel() >= 5)
			{
				htmltext = getHtm(player.getHtmlPrefix(), "noclanlevel.html");
			}
			else
			{
				player.getClan().changeLevel(5);
				return null;
			}
		}
		
		if (classLevelMenu > 0)
		{
			if (!isEnabled())
			{
				htmltext = getHtm(player.getHtmlPrefix(), "disabled.html");
			}
			else if (!validateClassChange(player, -1, false, false))
			{
				htmltext = "<html><body>You cannot change your occupation.</body></html>";
			}
			else
			{
				final ClassId currentClassId = player.getClassId();
				if (currentClassId.level() >= classLevelMenu)
				{
					htmltext = getHtm(player.getHtmlPrefix(), "nomore.html");
				}
				else
				{
					final int minLevel = getMinLevel(classLevelMenu - 1);
					if ((player.getLevel() >= minLevel) || isShowEntireTree())
					{
						final StringBuilder menu = new StringBuilder(100);
						
						//@formatter:off
						Arrays.stream(ClassId.values())
						.filter(cid -> validateClassId(player, cid))
						.forEach(cid -> menu.append("<a action=\"bypass -h Quest ClassMaster change_class " + cid.getId() + "\">" + ClassListData.getInstance().getClass(cid).getClientCode() + "</a><br>"));
						//@formatter:on
						
						if (menu.length() > 0)
						{
							htmltext = getHtm(player.getHtmlPrefix(), "template.html").replace("%name%", ClassListData.getInstance().getClass(currentClassId).getClientCode()).replace("%menu%", menu.toString());
						}
						else
						{
							htmltext = getHtm(player.getHtmlPrefix(), "comebacklater.html").replace("%level%", String.valueOf(getMinLevel(classLevelMenu - 1)));
						}
					}
					else
					{
						if (minLevel < Integer.MAX_VALUE)
						{
							htmltext = getHtm(player.getHtmlPrefix(), "comebacklater.html").replace("%level%", String.valueOf(minLevel));
						}
						else
						{
							htmltext = getHtm(player.getHtmlPrefix(), "nomore.html");
						}
					}
				}
			}
		}
		
		return htmltext;
	}
	
	@RegisterEvent(EventType.ON_PLAYER_PRESS_TUTORIAL_MARK)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerPressTutorialMark(OnPlayerPressTutorialMark event)
	{
		final PlayerInstance player = event.getActiveChar();
		
		if ((event.getMarkId() != 1001) || !validateClassChange(player, -1, true, true))
		{
			return;
		}
		
		final ClassId currentClassId = player.getClassId();
		String msg = getHtm(player.getHtmlPrefix(), "tutorialtemplate.html");
		msg = msg.replaceAll("%name%", ClassListData.getInstance().getClass(currentClassId).getEscapedClientCode());
		
		final StringBuilder menu = new StringBuilder(100);
		
		//@formatter:off
		Arrays.stream(ClassId.values())
		.filter(cid -> validateClassId(player, cid))
		.forEach(cid -> menu.append("<a action=\"bypass -h Quest ClassMaster CO " + cid.getId() + "\">" + ClassListData.getInstance().getClass(cid).getEscapedClientCode() + "</a><br>"));
		//@formatter:on
		
		msg = msg.replaceAll("%menu%", menu.toString());
		player.sendPacket(new TutorialShowHtml(msg));
	}
	
	@RegisterEvent(EventType.ON_PLAYER_BYPASS)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerBypass(OnPlayerBypass event)
	{
		final PlayerInstance player = event.getActiveChar();
		
		if (!player.getFloodProtectors().getServerBypass().tryPerformAction("changeclass"))
		{
			return;
		}
		
		// Tutorial Bypass
		StringTokenizer st = new StringTokenizer(event.getCommand());
		if (st.countTokens() < 3)
		{
			return;
		}
		
		st.nextToken(); // Quest
		st.nextToken(); // Class Master
		if (!st.nextToken().startsWith("CO"))
		{
			return;
		}
		
		if (!st.hasMoreTokens())
		{
			player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
			player.clearHtmlActions(HtmlActionScope.TUTORIAL_HTML);
			return;
		}
		
		int selectedClassId = Integer.parseInt(st.nextToken());
		if (validateClassChange(player, selectedClassId, true, true))
		{
			if (st.hasMoreTokens())
			{
				int classDataIndex = Integer.parseInt(st.nextToken());
				checkAndChangeClass(player, selectedClassId, classDataIndex);
				player.sendPacket(TutorialCloseHtml.STATIC_PACKET);
				player.clearHtmlActions(HtmlActionScope.TUTORIAL_HTML);
				return;
			}
			
			String msg = getHtm(player.getHtmlPrefix(), "tutorialtemplateOptions.html");
			msg = msg.replaceAll("%name%", ClassListData.getInstance().getClass(selectedClassId).getEscapedClientCode());
			msg = msg.replaceAll("%options%", getClassChangeOptions(player, selectedClassId, true));
			player.sendPacket(new TutorialShowHtml(msg));
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_PROFESSION_CHANGE)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerProfessionChange(OnPlayerProfessionChange event)
	{
		final PlayerInstance player = event.getActiveChar();
		if (validateClassChange(player, -1, true, true))
		{
			player.sendPacket(new TutorialShowQuestionMark(1001));
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LEVEL_CHANGED)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerLevelChanged(OnPlayerLevelChanged event)
	{
		final PlayerInstance player = event.getActiveChar();
		if (validateClassChange(player, -1, true, true))
		{
			player.sendPacket(new TutorialShowQuestionMark(1001));
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerLogin(OnPlayerLogin event)
	{
		final PlayerInstance player = event.getActiveChar();
		if (validateClassChange(player, -1, true, true))
		{
			player.sendPacket(new TutorialShowQuestionMark(1001));
		}
	}
	
	private boolean checkAndChangeClass(PlayerInstance player, int classId, int classDataIndex)
	{
		final ClassChangeData data = getClassChangeData(classDataIndex);
		
		if (data == null)
		{
			return false;
		}
		
		if (!validateClassChange(player, classId, true, false))
		{
			return false;
		}
		
		if (!data.getCategories().stream().anyMatch(ct -> player.isInCategory(ct)))
		{
			return false;
		}
		
		// Weight/Inventory check
		if (!data.getItemsRewarded().isEmpty() && !player.isInventoryUnder90(false))
		{
			player.sendPacket(SystemMessageId.UNABLE_TO_PROCESS_THIS_REQUEST_UNTIL_YOUR_INVENTORY_S_WEIGHT_AND_SLOT_COUNT_ARE_LESS_THAN_80_PERCENT_OF_CAPACITY);
			return false;
		}
		
		// check if player have all required items for class transfer
		for (ItemHolder holder : data.getItemsRequired())
		{
			if (player.getInventory().getInventoryItemCount(holder.getId(), -1) < holder.getCount())
			{
				player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT2);
				return false;
			}
		}
		
		// get all required items for class transfer
		for (ItemChanceHolder holder : data.getItemsRequired())
		{
			// Lucky ones dont get the item removed if the item is with chance to be removed.
			if ((holder.getChance() < 100) && (Rnd.get(100) > holder.getChance()))
			{
				continue;
			}
			
			if (!player.destroyItemByItemId("ClassMaster", holder.getId(), holder.getCount(), player, true))
			{
				return false;
			}
		}
		
		// reward player with items
		for (ItemChanceHolder holder : data.getItemsRewarded())
		{
			if ((holder.getChance() >= 100) || (holder.getChance() > Rnd.get(100)))
			{
				player.addItem("ClassMaster", holder.getId(), holder.getCount(), player, true);
			}
		}
		
		player.setClassId(classId);
		
		if (data.isRewardNoblesse())
		{
			if (!player.isNoble())
			{
				player.sendMessage("You have obtained Noblesse status.");
			}
			
			player.setNoble(true);
		}
		
		if (data.isRewardHero())
		{
			if (!player.isHero())
			{
				player.sendMessage("You have obtained Hero status.");
			}
			
			player.setHero(true);
		}
		
		if (player.isSubClassActive())
		{
			player.getSubClasses().get(player.getClassIndex()).setClassId(player.getActiveClass());
		}
		else
		{
			player.setBaseClass(player.getActiveClass());
		}
		
		player.store(false); // Save player cause if server crashes before this char is saved, he will lose class and the money payed for class change.
		player.broadcastUserInfo();
		
		return true;
	}
	
	private String getClassChangeOptions(PlayerInstance player, int selectedClassId, boolean tutorialWindow)
	{
		final StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < getClassChangeData().size(); i++)
		{
			ClassChangeData option = getClassChangeData(i);
			if ((option == null) || !option.getCategories().stream().anyMatch(ct -> player.isInCategory(ct)))
			{
				continue;
			}
			
			sb.append("<tr><td><img src=L2UI_CT1.ChatBalloon_DF_TopCenter width=276 height=1 /></td></tr>");
			sb.append("<tr><td><table bgcolor=3f3f3f width=100%>");
			if (tutorialWindow)
			{
				sb.append("<tr><td align=center><a action=\"bypass -h Quest ClassMaster CO " + selectedClassId + " " + i + "\">" + option.getName() + ":</a></td></tr>");
			}
			else
			{
				sb.append("<tr><td align=center><a action=\"bypass -h Quest ClassMaster change_class " + selectedClassId + " " + i + "\">" + option.getName() + ":</a></td></tr>");
			}
			sb.append("<tr><td><table width=276>");
			sb.append("<tr><td>Requirements:</td></tr>");
			if (option.getItemsRequired().isEmpty())
			{
				sb.append("<tr><td><font color=LEVEL>Free</font></td></tr>");
			}
			else
			{
				option.getItemsRequired().forEach(ih ->
				{
					if (ih.getChance() >= 100)
					{
						sb.append("<tr><td><font color=\"LEVEL\">" + ih.getCount() + "</font></td><td>" + ItemTable.getInstance().getTemplate(ih.getId()).getName() + "</td><td width=30></td></tr>");
					}
					else
					{
						sb.append("<tr><td><font color=\"LEVEL\">" + ih.getCount() + "</font></td><td>" + ItemTable.getInstance().getTemplate(ih.getId()).getName() + "</td><td width=30><font color=LEVEL>" + ih.getChance() + "%</font></td></tr>");
					}
				});
			}
			sb.append("<tr><td>Rewards:</td></tr>");
			if (option.getItemsRewarded().isEmpty())
			{
				if (option.isRewardNoblesse())
				{
					sb.append("<tr><td><font color=\"LEVEL\">Noblesse status.</font></td></tr>");
				}
				
				if (option.isRewardHero())
				{
					sb.append("<tr><td><font color=\"LEVEL\">Hero status.</font></td></tr>");
				}
				
				if (!option.isRewardNoblesse() && !option.isRewardHero())
				{
					sb.append("<tr><td><font color=LEVEL>none</font></td></tr>");
				}
			}
			else
			{
				option.getItemsRewarded().forEach(ih ->
				{
					if (ih.getChance() >= 100)
					{
						sb.append("<tr><td><font color=\"LEVEL\">" + ih.getCount() + "</font></td><td>" + ItemTable.getInstance().getTemplate(ih.getId()).getName() + "</td><td width=30></td></tr>");
					}
					else
					{
						sb.append("<tr><td><font color=\"LEVEL\">" + ih.getCount() + "</font></td><td>" + ItemTable.getInstance().getTemplate(ih.getId()).getName() + "</td><td width=30><font color=LEVEL>" + ih.getChance() + "%</font></td></tr>");
					}
				});
				
				if (option.isRewardNoblesse())
				{
					sb.append("<tr><td><font color=\"LEVEL\">Noblesse status.</font></td></tr>");
				}
				if (option.isRewardHero())
				{
					sb.append("<tr><td><font color=\"LEVEL\">Hero status.</font></td></tr>");
				}
			}
			sb.append("</table></td></tr>");
			sb.append("</table></td></tr>");
			sb.append("<tr><td><img src=L2UI_CT1.ChatBalloon_DF_TopCenter width=276 height=1 /></td></tr>");
		}
		
		return sb.toString();
	}
	
	/**
	 * @param level - current skillId level (0 - start, 1 - first, etc)
	 * @return minimum player level required for next class transfer
	 */
	private static int getMinLevel(int level)
	{
		switch (level)
		{
			case 0:
				return 20;
			case 1:
				return 40;
			case 2:
				return 76;
			case 3:
				return 85;
			default:
				return Integer.MAX_VALUE;
		}
	}
	
	private boolean validateClassChange(PlayerInstance player, int newClassId, boolean validateLevel, boolean popup)
	{
		if (!isEnabled())
		{
			return false;
		}
		
		if (validateLevel && (player.getLevel() < (getMinLevel(player.getClassId().level()))))
		{
			if (!isShowEntireTree())
			{
				return false;
			}
		}
		
		// Make ClassId check also if classId is present
		if ((newClassId >= 0) && (newClassId < ClassId.values().length))
		{
			ClassId newCID = ClassId.values()[newClassId];
			if (!validateClassId(player, newCID))
			{
				return false;
			}
		}
		
		if (popup)
		{
			if (!isClassChangeAvailableShowPopup(player))
			{
				return false;
			}
		}
		else
		{
			if (!isClassChangeAvailable(player))
			{
				return false;
			}
		}
		
		return true;
	}
	
	private boolean validateClassId(PlayerInstance player, ClassId newCID)
	{
		if ((newCID == null) || (newCID.getRace() == null))
		{
			return false;
		}
		
		if ((newCID == ClassId.INSPECTOR) && (player.getTotalSubClasses() < 2))
		{
			return false;
		}
		
		if (_bannedClassIds.contains(newCID.getId()))
		{
			return false;
		}
		
		if (player.getClassId().equals(newCID.getParent()))
		{
			return true;
		}
		
		if (isShowEntireTree() && newCID.childOf(player.getClassId()))
		{
			return true;
		}
		
		return false;
	}
	
	private static class ClassChangeData
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
	
	private boolean isEnabled()
	{
		return _isEnabled;
	}
	
	private boolean isSpawnClassMasters()
	{
		return _spawnClassMasters;
	}
	
	private boolean isShowEntireTree()
	{
		return _showEntireTree;
	}
	
	private List<ClassChangeData> getClassChangeData()
	{
		return _classChangeData;
	}
	
	private boolean isClassChangeAvailableShowPopup(PlayerInstance player)
	{
		return getClassChangeData().stream().filter(ClassChangeData::isShowPopupWindow).flatMap(ccd -> ccd.getCategories().stream()).anyMatch(ct -> player.isInCategory(ct));
	}
	
	private boolean isClassChangeAvailable(PlayerInstance player)
	{
		return getClassChangeData().stream().flatMap(ccd -> ccd.getCategories().stream()).anyMatch(ct -> player.isInCategory(ct));
	}
	
	private ClassChangeData getClassChangeData(int index)
	{
		if ((index >= 0) && (index < _classChangeData.size()))
		{
			return _classChangeData.get(index);
		}
		
		return null;
	}
	
	public static void main(String[] args)
	{
		new ClassMaster();
	}
}