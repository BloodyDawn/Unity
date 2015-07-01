/*
 * Copyright (C) 2004-2015 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.bypasshandlers;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.l2junity.Config;
import org.l2junity.gameserver.data.sql.impl.NpcBufferTable;
import org.l2junity.gameserver.data.sql.impl.NpcBufferTable.NpcBufferData;
import org.l2junity.gameserver.data.xml.impl.MultisellData;
import org.l2junity.gameserver.enums.CategoryType;
import org.l2junity.gameserver.handler.IBypassHandler;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.actor.instance.L2OlympiadManagerInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.entity.Hero;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.olympiad.CompetitionType;
import org.l2junity.gameserver.model.olympiad.Olympiad;
import org.l2junity.gameserver.model.olympiad.OlympiadManager;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.network.client.send.ExHeroList;
import org.l2junity.gameserver.network.client.send.ExShowScreenMessage;
import org.l2junity.gameserver.network.client.send.InventoryUpdate;
import org.l2junity.gameserver.network.client.send.MagicSkillUse;
import org.l2junity.gameserver.network.client.send.NpcHtmlMessage;
import org.l2junity.gameserver.network.client.send.SystemMessage;
import org.l2junity.gameserver.network.client.send.string.NpcStringId;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;
import org.l2junity.gameserver.util.Util;

/**
 * @author DS
 */
public class OlympiadManagerLink implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"olympiaddesc",
		"olympiadnoble",
		"olybuff",
		"olympiad"
	};
	
	private static final String FEWER_THAN = "Fewer than " + String.valueOf(Config.ALT_OLY_REG_DISPLAY);
	private static final String MORE_THAN = "More than " + String.valueOf(Config.ALT_OLY_REG_DISPLAY);
	private static final int GATE_PASS = Config.ALT_OLY_COMP_RITEM;
	
	private static final int[] BUFFS =
	{
		14738, // Olympiad - Horn Melody
		14739, // Olympiad - Drum Melody
		14740, // Olympiad - Pipe Organ Melody
		14741, // Olympiad - Guitar Melody
		14742, // Olympiad - Harp Melody
		14743, // Olympiad - Lute Melody
		14744, // Olympiad - Knight's Harmony
		14745, // Olympiad - Warrior's Harmony
		14746, // Olympiad - Wizard's Harmony
	};
	
	@Override
	public final boolean useBypass(String command, PlayerInstance activeChar, Creature target)
	{
		if (!(target instanceof L2OlympiadManagerInstance))
		{
			return false;
		}
		
		try
		{
			if (command.toLowerCase().startsWith("olympiaddesc"))
			{
				int val = Integer.parseInt(command.substring(13, 14));
				String suffix = command.substring(14);
				((L2OlympiadManagerInstance) target).showChatWindow(activeChar, val, suffix);
			}
			else if (command.toLowerCase().startsWith("olympiadnoble"))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(target.getObjectId());
				if (activeChar.isCursedWeaponEquipped())
				{
					html.setFile(activeChar.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "noble_cursed_weapon.htm");
					activeChar.sendPacket(html);
					return false;
				}
				if (activeChar.getClassIndex() != 0)
				{
					html.setFile(activeChar.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "noble_sub.htm");
					html.replace("%objectId%", String.valueOf(target.getObjectId()));
					activeChar.sendPacket(html);
					return false;
				}
				if (!activeChar.isNoble() || !activeChar.isInCategory(CategoryType.AWAKEN_GROUP))
				{
					html.setFile(activeChar.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "noble_thirdclass.htm");
					html.replace("%objectId%", String.valueOf(target.getObjectId()));
					activeChar.sendPacket(html);
					return false;
				}
				
				int passes;
				int val = Integer.parseInt(command.substring(14));
				switch (val)
				{
					case 0: // H5 match selection
						if (!OlympiadManager.getInstance().isRegistered(activeChar))
						{
							html.setFile(activeChar.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "noble_desc2a.htm");
							html.replace("%objectId%", String.valueOf(target.getObjectId()));
							html.replace("%olympiad_period%", String.valueOf(Olympiad.getInstance().getPeriod()));
							html.replace("%olympiad_cycle%", String.valueOf(Olympiad.getInstance().getCurrentCycle()));
							html.replace("%olympiad_opponent%", String.valueOf(OlympiadManager.getInstance().getCountOpponents()));
							activeChar.sendPacket(html);
						}
						else
						{
							html.setFile(activeChar.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "noble_unregister.htm");
							html.replace("%objectId%", String.valueOf(target.getObjectId()));
							activeChar.sendPacket(html);
						}
						break;
					case 1: // unregister
						OlympiadManager.getInstance().unRegisterNoble(activeChar);
						break;
					case 2: // show waiting list | TODO: cleanup (not used anymore)
						final int nonClassed = OlympiadManager.getInstance().getRegisteredNonClassBased().size();
						final Collection<Set<Integer>> allClassed = OlympiadManager.getInstance().getRegisteredClassBased().values();
						int classed = 0;
						if (!allClassed.isEmpty())
						{
							for (Set<Integer> cls : allClassed)
							{
								if (cls != null)
								{
									classed += cls.size();
								}
							}
						}
						html.setFile(activeChar.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "noble_registered.htm");
						if (Config.ALT_OLY_REG_DISPLAY > 0)
						{
							html.replace("%listClassed%", classed < Config.ALT_OLY_REG_DISPLAY ? FEWER_THAN : MORE_THAN);
							html.replace("%listNonClassedTeam%", FEWER_THAN); // TODO: Cleanup
							html.replace("%listNonClassed%", nonClassed < Config.ALT_OLY_REG_DISPLAY ? FEWER_THAN : MORE_THAN);
						}
						else
						{
							html.replace("%listClassed%", String.valueOf(classed));
							html.replace("%listNonClassedTeam%", String.valueOf(0)); // TODO: Cleanup
							html.replace("%listNonClassed%", String.valueOf(nonClassed));
						}
						html.replace("%objectId%", String.valueOf(target.getObjectId()));
						activeChar.sendPacket(html);
						break;
					case 3: // There are %points% Grand Olympiad points granted for this event. | TODO: cleanup (not used anymore)
						int points = Olympiad.getInstance().getNoblePoints(activeChar.getObjectId());
						html.setFile(activeChar.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "noble_points1.htm");
						html.replace("%points%", String.valueOf(points));
						html.replace("%objectId%", String.valueOf(target.getObjectId()));
						activeChar.sendPacket(html);
						break;
					case 4: // register non classed
						OlympiadManager.getInstance().registerNoble(activeChar, CompetitionType.NON_CLASSED);
						break;
					case 5: // register classed
						OlympiadManager.getInstance().registerNoble(activeChar, CompetitionType.CLASSED);
						break;
					case 6: // request tokens reward
						passes = Olympiad.getInstance().getNoblessePasses(activeChar, false);
						if (passes > 0)
						{
							html.setFile(activeChar.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "noble_settle.htm");
							html.replace("%objectId%", String.valueOf(target.getObjectId()));
							activeChar.sendPacket(html);
						}
						else
						{
							html.setFile(activeChar.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "noble_nopoints2.htm");
							html.replace("%objectId%", String.valueOf(target.getObjectId()));
							activeChar.sendPacket(html);
						}
						break;
					case 7: // Equipment Rewards
						MultisellData.getInstance().separateAndSend(102, activeChar, (Npc) target, false);
						break;
					case 8: // Misc. Rewards
						MultisellData.getInstance().separateAndSend(103, activeChar, (Npc) target, false);
						break;
					case 9: // Your Grand Olympiad Score from the previous period is %points% point(s) | TODO: cleanup (not used anymore)
						int point = Olympiad.getInstance().getLastNobleOlympiadPoints(activeChar.getObjectId());
						html.setFile(activeChar.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "noble_points2.htm");
						html.replace("%points%", String.valueOf(point));
						html.replace("%objectId%", String.valueOf(target.getObjectId()));
						activeChar.sendPacket(html);
						break;
					case 10: // give tokens to player
						passes = Olympiad.getInstance().getNoblessePasses(activeChar, true);
						if (passes > 0)
						{
							ItemInstance item = activeChar.getInventory().addItem("Olympiad", GATE_PASS, passes, activeChar, target);
							
							InventoryUpdate iu = new InventoryUpdate();
							iu.addModifiedItem(item);
							activeChar.sendInventoryUpdate(iu);
							
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
							sm.addLong(passes);
							sm.addItemName(item);
							activeChar.sendPacket(sm);
						}
						break;
					default:
						_log.warn("Olympiad System: Couldnt send packet for request " + val);
						break;
				}
			}
			else if (command.toLowerCase().startsWith("olybuff"))
			{
				int buffCount = activeChar.getOlympiadBuffCount();
				if (buffCount <= 0)
				{
					return false;
				}
				
				final NpcHtmlMessage html = new NpcHtmlMessage(target.getObjectId());
				String[] params = command.split(" ");
				
				if (!Util.isDigit(params[1]))
				{
					_log.warn("Olympiad Buffer Warning: npcId = " + target.getId() + " has invalid buffGroup set in the bypass for the buff selected: " + params[1]);
					return false;
				}
				
				final int index = Integer.parseInt(params[1]);
				if ((index < 0) || (index > BUFFS.length))
				{
					_log.warn("Olympiad Buffer Warning: npcId = " + target.getId() + " has invalid index sent in the bypass: " + index);
					return false;
				}
				
				final NpcBufferData npcBuffGroupInfo = NpcBufferTable.getInstance().getSkillInfo(target.getId(), BUFFS[index]);
				if (npcBuffGroupInfo == null)
				{
					_log.warn("Olympiad Buffer Warning: npcId = " + target.getId() + " Location: " + target.getX() + ", " + target.getY() + ", " + target.getZ() + " Player: " + activeChar.getName() + " has tried to use skill group (" + params[1] + ") not assigned to the NPC Buffer!");
					return false;
				}
				
				if (buffCount > 0)
				{
					final Skill skill = npcBuffGroupInfo.getSkill().getSkill();
					if (skill != null)
					{
						target.setTarget(activeChar);
						
						activeChar.setOlympiadBuffCount(--buffCount);
						
						target.broadcastPacket(new MagicSkillUse(target, activeChar, skill.getId(), skill.getLevel(), 0, 0));
						skill.applyEffects(activeChar, activeChar);
						final Summon pet = activeChar.getPet();
						if (pet != null)
						{
							target.broadcastPacket(new MagicSkillUse(target, pet, skill.getId(), skill.getLevel(), 0, 0));
							skill.applyEffects(pet, pet);
						}
						activeChar.getServitors().values().forEach(s ->
						{
							target.broadcastPacket(new MagicSkillUse(target, s, skill.getId(), skill.getLevel(), 0, 0));
							skill.applyEffects(s, s);
						});
					}
				}
				
				if (buffCount > 0)
				{
					html.setFile(activeChar.getHtmlPrefix(), buffCount == Config.ALT_OLY_MAX_BUFFS ? Olympiad.OLYMPIAD_HTML_PATH + "olympiad_buffs.htm" : Olympiad.OLYMPIAD_HTML_PATH + "olympiad_5buffs.htm");
					html.replace("%objectId%", String.valueOf(target.getObjectId()));
					activeChar.sendPacket(html);
				}
				else
				{
					html.setFile(activeChar.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "olympiad_nobuffs.htm");
					html.replace("%objectId%", String.valueOf(target.getObjectId()));
					activeChar.sendPacket(html);
					target.decayMe();
				}
			}
			else if (command.toLowerCase().startsWith("olympiad"))
			{
				int val = Integer.parseInt(command.substring(9, 10));
				
				final NpcHtmlMessage reply = new NpcHtmlMessage(target.getObjectId());
				
				switch (val)
				{
					case 2: // show rank for a specific class
						// for example >> Olympiad 1_88
						int classId = Integer.parseInt(command.substring(11));
						if (((classId >= 88) && (classId <= 118)) || ((classId >= 131) && (classId <= 134)) || (classId == 136))
						{
							List<String> names = Olympiad.getInstance().getClassLeaderBoard(classId);
							reply.setFile(activeChar.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "olympiad_ranking.htm");
							
							int index = 1;
							for (String name : names)
							{
								reply.replace("%place" + index + "%", String.valueOf(index));
								reply.replace("%rank" + index + "%", name);
								index++;
								if (index > 10)
								{
									break;
								}
							}
							for (; index <= 10; index++)
							{
								reply.replace("%place" + index + "%", "");
								reply.replace("%rank" + index + "%", "");
							}
							
							reply.replace("%objectId%", String.valueOf(target.getObjectId()));
							activeChar.sendPacket(reply);
						}
						break;
					case 4: // hero list
						activeChar.sendPacket(new ExHeroList());
						break;
					case 5: // Hero Certification
						if (Hero.getInstance().isUnclaimedHero(activeChar.getObjectId()))
						{
							Hero.getInstance().claimHero(activeChar);
							reply.setFile(activeChar.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "hero_receive.htm");
							activeChar.sendPacket(new ExShowScreenMessage(NpcStringId.getNpcStringId(13357 + activeChar.getClassId().getId()), 5000, ExShowScreenMessage.TOP_CENTER, activeChar.getName()));
						}
						else
						{
							reply.setFile(activeChar.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "hero_notreceive.htm");
						}
						activeChar.sendPacket(reply);
						break;
					default:
						_log.warn("Olympiad System: Couldnt send packet for request " + val);
						break;
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("Exception in " + getClass().getSimpleName(), e);
		}
		
		return true;
	}
	
	@Override
	public final String[] getBypassList()
	{
		return COMMANDS;
	}
}
