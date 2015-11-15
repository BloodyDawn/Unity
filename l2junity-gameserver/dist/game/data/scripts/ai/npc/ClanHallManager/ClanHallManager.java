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
package ai.npc.ClanHallManager;

import java.util.StringTokenizer;

import org.l2junity.commons.util.CommonUtil;
import org.l2junity.gameserver.model.ClanPrivilege;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.L2MerchantInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.entity.ClanHall;
import org.l2junity.gameserver.model.holders.ClanHallTeleportHolder;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.model.residences.ResidenceFunctionType;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

import ai.npc.AbstractNpcAI;

/**
 * Clan Hall Manager AI.
 * @author St3eT
 */
public final class ClanHallManager extends AbstractNpcAI
{
	// NPCs
	// @formatter:off
	private static final int[] CLANHALL_MANAGERS =
	{
		35384, 35386, 35388, // Gludio
		36725, 36723, 36721, 36727, // Gludio Outskirts
		35455, 35453, 35451, 35457, 35459, // Giran
		35441, 35439, 35443, 35447, 35449, 35445, // Aden
		35467, 35465, 35463, 35461, // Goddard
		35578, 35576, 35574, 35566, 35572, 35570, 35568, // Rune
		35407, 35405, 35403, // Dion
		35394, 35396, 35398, 35400, 35392, // Gludin
	};
	// @formatter:on
	// Misc
	private static final int[] ALLOWED_BUFFS =
	{
		4342, // Wind Walk
		4343, // Decrease Weight
		4344, // Shield
		4346, // Mental Shield
		4345, // Might
		15374, // Horn Melody
		15375, // Drum Melody
		4347, // Blessed Body
		4349, // Magic Barrier
		4350, // Resist Shock
		4348, // Blessed Soul
		15376, // Pipe Organ Melody
		15377, // Guitar Melody
		4351, // Concentration
		4352, // Berserker Spirit
		4353, // Blessed Shield
		4358, // Guidance
		4354, // Vampiric Rage
		15378, // Harp Melody
		15379, // Lute Melody
		15380, // Knight's Harmony
		15381, // Warrior's Harmony
		15382, // Wizard's Harmony
		4355, // Acumen
		4356, // Empower
		4357, // Haste
		4359, // Focus
		4360, // Death Whisper
	};
	
	private ClanHallManager()
	{
		super(ClanHallManager.class.getSimpleName(), "ai/npc");
		addStartNpc(CLANHALL_MANAGERS);
		addTalkId(CLANHALL_MANAGERS);
		addFirstTalkId(CLANHALL_MANAGERS);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final StringTokenizer st = new StringTokenizer(event, " ");
		final String action = st.nextToken();
		final ClanHall clanHall = npc.getClanHall();
		String htmltext = null;
		
		if ((clanHall != null) && isOwningClan(player, npc))
		{
			switch (action)
			{
				case "index":
				{
					htmltext = isOwningClan(player, npc) ? "ClanHallManager-01.html" : "ClanHallManager-03.html";
					break;
				}
				case "manageDoors":
				{
					if (player.hasClanPrivilege(ClanPrivilege.CH_OPEN_DOOR))
					{
						if (st.hasMoreTokens())
						{
							final boolean open = st.nextToken().equals("1");
							clanHall.openCloseDoors(open);
							htmltext = "ClanHallManager-0" + (open ? "5" : "6") + ".html";
						}
						else
						{
							htmltext = "ClanHallManager-04.html";
						}
					}
					else
					{
						htmltext = "ClanHallManager-noAuthority.html";
					}
					break;
				}
				case "expel":
				{
					if (player.hasClanPrivilege(ClanPrivilege.CH_DISMISS))
					{
						if (st.hasMoreTokens())
						{
							clanHall.banishOthers();
							htmltext = "ClanHallManager-08.html";
						}
						else
						{
							htmltext = "ClanHallManager-07.html";
						}
					}
					else
					{
						htmltext = "ClanHallManager-noAuthority.html";
					}
					break;
				}
				case "useFunctions":
				{
					if (player.hasClanPrivilege(ClanPrivilege.CH_OTHER_RIGHTS))
					{
						if (!st.hasMoreTokens())
						{
							htmltext = getHtm(player.getHtmlPrefix(), "ClanHallManager-09.html");
							htmltext = htmltext.replaceAll("%hpFunction%", String.valueOf(clanHall.getFunctionLevel(ResidenceFunctionType.HP_REGEN)));
							htmltext = htmltext.replaceAll("%mpFunction%", String.valueOf(clanHall.getFunctionLevel(ResidenceFunctionType.MP_REGEN)));
							htmltext = htmltext.replaceAll("%resFunction%", String.valueOf(clanHall.getFunctionLevel(ResidenceFunctionType.EXP_RESTORE)));
						}
						else
						{
							switch (st.nextToken())
							{
								case "teleport":
								{
									final int teleportLevel = clanHall.getFunctionLevel(ResidenceFunctionType.TELEPORT) - 10;
									if (teleportLevel > 0)
									{
										if (!st.hasMoreTokens())
										{
											final StringBuilder sb = new StringBuilder();
											htmltext = getHtm(player.getHtmlPrefix(), "ClanHallManager-funcTeleport.html");
											// Generate teleport list
											clanHall.getTeleportList(teleportLevel).forEach(teleport ->
											{
												final String price = (teleport.getCost() > 0) ? (" - " + teleport.getCost() + " Adena") : "";
												sb.append("<button align=left icon=\"teleport\" action=\"bypass -h Quest ClanHallManager useFunctions teleport " + teleport.getNpcStringId().getId() + "\" msg=\"811;F;" + teleport.getNpcStringId().getId() + "\"><fstring>" + teleport.getNpcStringId().getId() + "</fstring>" + price + "</button>");
											});
											htmltext = htmltext.replaceAll("%teleportList%", sb.toString());
										}
										else
										{
											final int destination = Integer.parseInt(st.nextToken());
											final ClanHallTeleportHolder holder = clanHall.getTeleportList(teleportLevel).stream().filter(tel -> tel.getNpcStringId().getId() == destination).findFirst().orElse(null);
											if (holder != null)
											{
												if (player.getAdena() >= holder.getCost())
												{
													player.reduceAdena("Clan Hall Teleport", holder.getCost(), npc, true);
													player.teleToLocation(holder.getLocation());
												}
												else
												{
													player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
												}
											}
											else
											{
												htmltext = "ClanHallManager-noFunction.html";
											}
										}
									}
									else
									{
										htmltext = "ClanHallManager-noFunction.html";
									}
									break;
								}
								case "buffs":
								{
									final int buffLevel = clanHall.getFunctionLevel(ResidenceFunctionType.BUFF) - 14;
									if (buffLevel > 0)
									{
										if (!st.hasMoreTokens())
										{
											htmltext = getHtm(player.getHtmlPrefix(), "ClanHallManager-funcBuffs_" + buffLevel + ".html");
											htmltext = htmltext.replaceAll("%manaLeft%", Integer.toString((int) npc.getCurrentMp()));
										}
										else
										{
											final String[] skillData = st.nextToken().split("_");
											final SkillHolder skill = new SkillHolder(Integer.parseInt(skillData[0]), Integer.parseInt(skillData[1]));
											if (CommonUtil.contains(ALLOWED_BUFFS, skill.getSkillId()))
											{
												if (npc.getCurrentMp() < (npc.getStat().getMpConsume(skill.getSkill()) + npc.getStat().getMpInitialConsume(skill.getSkill())))
												{
													htmltext = getHtm(player.getHtmlPrefix(), "ClanHallManager-funcBuffsNoMp.html");
												}
												else if (npc.isSkillDisabled(skill.getSkill()))
												{
													htmltext = getHtm(player.getHtmlPrefix(), "ClanHallManager-funcBuffsNoReuse.html");
												}
												else
												{
													castSkill(npc, player, skill);
													htmltext = getHtm(player.getHtmlPrefix(), "ClanHallManager-funcBuffsDone.html");
												}
												htmltext = htmltext.replaceAll("%manaLeft%", Integer.toString((int) npc.getCurrentMp()));
											}
										}
									}
									else
									{
										htmltext = "ClanHallManager-noFunction.html";
									}
									break;
								}
								case "items":
								{
									final int itemLevel = clanHall.getFunctionLevel(ResidenceFunctionType.BUFF) - 10;
									switch (itemLevel)
									{
										case 1:
										case 2:
										case 3:
											((L2MerchantInstance) npc).showBuyWindow(player, Integer.parseInt(npc.getId() + "0" + (itemLevel - 1)));
											break;
										default:
											htmltext = "ClanHallManager-noFunction.html";
									}
									break;
								}
							}
						}
					}
					else
					{
						htmltext = "ClanHallManager-noAuthority.html";
					}
					break;
				}
				case "manageFunctions":
				{
				
				}
				default:
				{
					player.sendMessage("This case is not implemented yet. :(");
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, PlayerInstance player)
	{
		String htmltext = null;
		final ClanHall clanHall = npc.getClanHall();
		
		if (isOwningClan(player, npc))
		{
			if (clanHall.getCostFailDay() == 0)
			{
				htmltext = "ClanHallManager-01.html";
			}
			else
			{
				htmltext = getHtm(player.getHtmlPrefix(), "ClanHallManager-02.html");
				htmltext = htmltext.replaceAll("%costFailDayLeft%", Integer.toString((8 - clanHall.getCostFailDay())));
			}
		}
		else
		{
			htmltext = "ClanHallManager-03.html";
		}
		return htmltext;
	}
	
	private boolean isOwningClan(PlayerInstance player, Npc npc)
	{
		return ((npc.getClanHall().getOwnerId() == player.getClanId()) && (player.getClanId() != 0));
	}
	
	public static void main(String[] args)
	{
		new ClanHallManager();
	}
}