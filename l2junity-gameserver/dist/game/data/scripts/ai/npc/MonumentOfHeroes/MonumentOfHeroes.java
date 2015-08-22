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
package ai.npc.MonumentOfHeroes;

import java.util.List;

import org.l2junity.commons.util.CommonUtil;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.olympiad.Olympiad;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

import ai.npc.AbstractNpcAI;

/**
 * Monument of Heroes AI.
 * @author Adry_85
 */
public final class MonumentOfHeroes extends AbstractNpcAI
{
	// NPCs
	private static final int[] MONUMENTS =
	{
		31690,
		31769,
		31770,
		31771,
		31772
	};
	// Items
	private static final int HERO_CLOAK = 30372;
	private static final int GLORIOUS_CLOAK = 30373;
	private static final int WINGS_OF_DESTINY_CIRCLET = 6842;
	private static final int[] WEAPONS =
	{
		6611, // Infinity Blade
		6612, // Infinity Cleaver
		6613, // Infinity Axe
		6614, // Infinity Rod
		6615, // Infinity Crusher
		6616, // Infinity Scepter
		6617, // Infinity Stinger
		6618, // Infinity Fang
		6619, // Infinity Bow
		6620, // Infinity Wing
		6621, // Infinity Spear
		9388, // Infinity Rapier
		9389, // Infinity Sword
		9390, // Infinity Shooter
	};
	
	private MonumentOfHeroes()
	{
		super(MonumentOfHeroes.class.getSimpleName(), "ai/npc");
		addStartNpc(MONUMENTS);
		addTalkId(MONUMENTS);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		String htmltext = null;
		
		switch (event)
		{
			case "receiveCloak":
			{
				final int olympiadRank = getOlympiadRank(player);
				
				if (olympiadRank == 1)
				{
					if (!hasAtLeastOneQuestItem(player, HERO_CLOAK, GLORIOUS_CLOAK))
					{
						if (player.isInventoryUnder80(false))
						{
							player.sendPacket(SystemMessageId.UNABLE_TO_PROCESS_THIS_REQUEST_UNTIL_YOUR_INVENTORY_S_WEIGHT_AND_SLOT_COUNT_ARE_LESS_THAN_80_PERCENT_OF_CAPACITY);
						}
						else
						{
							giveItems(player, HERO_CLOAK, 1);
						}
					}
					else
					{
						htmltext = "cloak_already_have.html";
					}
				}
				else if ((olympiadRank == 2) || (olympiadRank == 3))
				{
					if (!hasAtLeastOneQuestItem(player, HERO_CLOAK, GLORIOUS_CLOAK))
					{
						if (player.isInventoryUnder80(false))
						{
							player.sendPacket(SystemMessageId.UNABLE_TO_PROCESS_THIS_REQUEST_UNTIL_YOUR_INVENTORY_S_WEIGHT_AND_SLOT_COUNT_ARE_LESS_THAN_80_PERCENT_OF_CAPACITY);
						}
						else
						{
							giveItems(player, GLORIOUS_CLOAK, 1);
						}
					}
					else
					{
						htmltext = "cloak_already_have.html";
					}
				}
				else
				{
					htmltext = "cloak_no_rank.html";
				}
				break;
			}
			case "HeroWeapon":
			{
				if (player.isHero())
				{
					htmltext = hasAtLeastOneQuestItem(player, WEAPONS) ? "already_have_weapon.htm" : "weapon_list.htm";
				}
				else
				{
					htmltext = "no_hero_weapon.htm";
				}
			}
			case "HeroCirclet":
			{
				if (player.isHero())
				{
					if (!hasQuestItems(player, WINGS_OF_DESTINY_CIRCLET))
					{
						giveItems(player, WINGS_OF_DESTINY_CIRCLET, 1);
					}
					else
					{
						htmltext = "already_have_circlet.htm";
					}
				}
				else
				{
					htmltext = "no_hero_circlet.htm";
				}
				break;
			}
			default:
			{
				int weaponId = Integer.parseInt(event);
				if (CommonUtil.contains(WEAPONS, weaponId))
				{
					giveItems(player, weaponId, 1);
				}
				break;
			}
		}
		return htmltext;
	}
	
	private int getOlympiadRank(PlayerInstance player)
	{
		final List<String> names = Olympiad.getInstance().getClassLeaderBoard(player.getClassId().getId());
		for (int i = 1; i <= 3; i++)
		{
			if (names.get(i - 1).equals(player.getName()))
			{
				return i;
			}
		}
		return -1;
	}
	
	public static void main(String[] args)
	{
		new MonumentOfHeroes();
	}
}