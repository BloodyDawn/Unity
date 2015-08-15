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
package org.l2junity.gameserver.network.client.recv;

import java.util.Arrays;

import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.ai.CtrlIntention;
import org.l2junity.gameserver.ai.SummonAI;
import org.l2junity.gameserver.data.sql.impl.SummonSkillsTable;
import org.l2junity.gameserver.data.xml.impl.ActionData;
import org.l2junity.gameserver.data.xml.impl.PetDataTable;
import org.l2junity.gameserver.datatables.SkillData;
import org.l2junity.gameserver.enums.ChatType;
import org.l2junity.gameserver.enums.MountType;
import org.l2junity.gameserver.enums.PrivateStoreType;
import org.l2junity.gameserver.handler.IPlayerActionHandler;
import org.l2junity.gameserver.handler.PlayerActionHandler;
import org.l2junity.gameserver.model.ActionDataHolder;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.actor.instance.L2PetInstance;
import org.l2junity.gameserver.model.actor.instance.L2SiegeFlagInstance;
import org.l2junity.gameserver.model.actor.instance.L2StaticObjectInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.skills.AbnormalType;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.network.client.L2GameClient;
import org.l2junity.gameserver.network.client.send.ActionFailed;
import org.l2junity.gameserver.network.client.send.ChairSit;
import org.l2junity.gameserver.network.client.send.ExBasicActionList;
import org.l2junity.gameserver.network.client.send.NpcSay;
import org.l2junity.gameserver.network.client.send.RecipeShopManageList;
import org.l2junity.gameserver.network.client.send.string.NpcStringId;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;
import org.l2junity.network.PacketReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the action use request packet.
 * @author Zoey76
 */
public final class RequestActionUse implements IClientIncomingPacket
{
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestActionUse.class);
	
	private static final int SIN_EATER_ID = 12564;
	private static final int SWITCH_STANCE_ID = 6054;
	private static final NpcStringId[] NPC_STRINGS =
	{
		NpcStringId.USING_A_SPECIAL_SKILL_HERE_COULD_TRIGGER_A_BLOODBATH,
		NpcStringId.HEY_WHAT_DO_YOU_EXPECT_OF_ME,
		NpcStringId.UGGGGGH_PUSH_IT_S_NOT_COMING_OUT,
		NpcStringId.AH_I_MISSED_THE_MARK
	};
	
	private int _actionId;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;
	
	@Override
	public boolean read(L2GameClient client, PacketReader packet)
	{
		_actionId = packet.readD();
		_ctrlPressed = (packet.readD() == 1);
		_shiftPressed = (packet.readC() == 1);
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		final PlayerInstance activeChar = client.getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		// Don't do anything if player is dead or confused
		if ((activeChar.isFakeDeath() && (_actionId != 0)) || activeChar.isDead() || activeChar.isControlBlocked())
		{
			client.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final BuffInfo info = activeChar.getEffectList().getBuffInfoByAbnormalType(AbnormalType.BOT_PENALTY);
		if (info != null)
		{
			for (AbstractEffect effect : info.getEffects())
			{
				if (!effect.checkCondition(_actionId))
				{
					activeChar.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REPORTED_AS_AN_ILLEGAL_PROGRAM_USER_SO_YOUR_ACTIONS_HAVE_BEEN_RESTRICTED);
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
		}
		
		// Don't allow to do some action if player is transformed
		if (activeChar.isTransformed())
		{
			int[] allowedActions = activeChar.isTransformed() ? ExBasicActionList.ACTIONS_ON_TRANSFORM : ExBasicActionList.DEFAULT_ACTION_LIST;
			if (!(Arrays.binarySearch(allowedActions, _actionId) >= 0))
			{
				client.sendPacket(ActionFailed.STATIC_PACKET);
				_log.warn("Player " + activeChar + " used action which he does not have! Id = " + _actionId + " transform: " + activeChar.getTransformation());
				return;
			}
		}
		
		final ActionDataHolder actionHolder = ActionData.getInstance().getActionData(_actionId);
		if (actionHolder != null)
		{
			final IPlayerActionHandler actionHandler = PlayerActionHandler.getInstance().getHandler(actionHolder.getHandler());
			if (actionHandler != null)
			{
				actionHandler.useAction(activeChar, actionHolder, _ctrlPressed, _shiftPressed);
				return;
			}
			LOGGER.warn("Couldnt find handler with name: {}", actionHolder.getHandler());
			return;
		}
		
		final Summon pet = activeChar.getPet();
		final Summon servitor = activeChar.getAnyServitor();
		final WorldObject target = activeChar.getTarget();
		switch (_actionId)
		{
			case 15: // Change Movement Mode (Pets)
				if (validateSummon(activeChar, pet, true))
				{
					((SummonAI) pet.getAI()).notifyFollowStatusChange();
				}
				break;
			case 16: // Attack (Pets)
				if (validateSummon(activeChar, pet, true))
				{
					if (pet.canAttack(activeChar.getTarget(), _ctrlPressed))
					{
						pet.doAttack(activeChar.getTarget());
					}
				}
				break;
			case 17: // Stop (Pets)
				if (validateSummon(activeChar, pet, true))
				{
					pet.cancelAction();
				}
				break;
			case 21: // Change Movement Mode (Servitors)
				if (validateSummon(activeChar, servitor, false))
				{
					((SummonAI) servitor.getAI()).notifyFollowStatusChange();
				}
				break;
			case 23: // Stop (Servitors)
				if (validateSummon(activeChar, servitor, false))
				{
					servitor.cancelAction();
				}
				break;
			case 32: // Wild Hog Cannon - Wild Cannon
				useSkill(activeChar, 4230, false);
				break;
			case 36: // Soulless - Toxic Smoke
				useSkill(activeChar, 4259, false);
				break;
			case 38: // Mount/Dismount
				activeChar.mountPlayer(pet);
				break;
			case 39: // Soulless - Parasite Burst
				useSkill(activeChar, 4138, false);
				break;
			case 41: // Wild Hog Cannon - Attack
				if (validateSummon(activeChar, servitor, false))
				{
					if ((target != null) && (target.isDoor() || (target instanceof L2SiegeFlagInstance)))
					{
						useSkill(activeChar, 4230, false);
					}
					else
					{
						client.sendPacket(SystemMessageId.INVALID_TARGET);
					}
				}
				break;
			case 42: // Kai the Cat - Self Damage Shield
				useSkill(activeChar, 4378, activeChar, false);
				break;
			case 43: // Unicorn Merrow - Hydro Screw
				useSkill(activeChar, 4137, false);
				break;
			case 44: // Big Boom - Boom Attack
				useSkill(activeChar, 4139, false);
				break;
			case 45: // Unicorn Boxer - Master Recharge
				useSkill(activeChar, 4025, activeChar, false);
				break;
			case 46: // Mew the Cat - Mega Storm Strike
				useSkill(activeChar, 4261, false);
				break;
			case 47: // Silhouette - Steal Blood
				useSkill(activeChar, 4260, false);
				break;
			case 48: // Mechanic Golem - Mech. Cannon
				useSkill(activeChar, 4068, false);
				break;
			case 51: // General Manufacture
				// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
				if (activeChar.isAlikeDead())
				{
					client.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				if (activeChar.getPrivateStoreType() != PrivateStoreType.NONE)
				{
					activeChar.setPrivateStoreType(PrivateStoreType.NONE);
					activeChar.broadcastUserInfo();
				}
				if (activeChar.isSitting())
				{
					activeChar.standUp();
				}
				
				client.sendPacket(new RecipeShopManageList(activeChar, false));
				break;
			case 53: // Move to target (Servitors)
				if (validateSummon(activeChar, servitor, false))
				{
					if ((target != null) && (servitor != target) && !servitor.isMovementDisabled())
					{
						servitor.setFollowStatus(false);
						servitor.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, target.getLocation());
					}
				}
				break;
			case 54: // Move to target (Pets)
				if (validateSummon(activeChar, pet, true))
				{
					if ((target != null) && (pet != target) && !pet.isMovementDisabled())
					{
						pet.setFollowStatus(false);
						pet.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, target.getLocation());
					}
				}
				break;
			case 1000: // Siege Golem - Siege Hammer
				if ((target != null) && target.isDoor())
				{
					useSkill(activeChar, 4079, false);
				}
				break;
			case 1001: // Sin Eater - Ultimate Bombastic Buster
				if (validateSummon(activeChar, pet, true) && (pet.getId() == SIN_EATER_ID))
				{
					pet.broadcastPacket(new NpcSay(pet.getObjectId(), ChatType.NPC_GENERAL, pet.getId(), NPC_STRINGS[Rnd.get(NPC_STRINGS.length)]));
				}
				break;
			case 1003: // Wind Hatchling/Strider - Wild Stun
				useSkill(activeChar, 4710, true);
				break;
			case 1004: // Wind Hatchling/Strider - Wild Defense
				useSkill(activeChar, 4711, activeChar, true);
				break;
			case 1005: // Star Hatchling/Strider - Bright Burst
				useSkill(activeChar, 4712, true);
				break;
			case 1006: // Star Hatchling/Strider - Bright Heal
				useSkill(activeChar, 4713, activeChar, true);
				break;
			case 1007: // Cat Queen - Blessing of Queen
				useSkill(activeChar, 4699, activeChar, false);
				break;
			case 1008: // Cat Queen - Gift of Queen
				useSkill(activeChar, 4700, activeChar, false);
				break;
			case 1009: // Cat Queen - Cure of Queen
				useSkill(activeChar, 4701, false);
				break;
			case 1010: // Unicorn Seraphim - Blessing of Seraphim
				useSkill(activeChar, 4702, activeChar, false);
				break;
			case 1011: // Unicorn Seraphim - Gift of Seraphim
				useSkill(activeChar, 4703, activeChar, false);
				break;
			case 1012: // Unicorn Seraphim - Cure of Seraphim
				useSkill(activeChar, 4704, false);
				break;
			case 1013: // Nightshade - Curse of Shade
				useSkill(activeChar, 4705, false);
				break;
			case 1014: // Nightshade - Mass Curse of Shade
				useSkill(activeChar, 4706, false);
				break;
			case 1015: // Nightshade - Shade Sacrifice
				useSkill(activeChar, 4707, false);
				break;
			case 1016: // Cursed Man - Cursed Blow
				useSkill(activeChar, 4709, false);
				break;
			case 1017: // Cursed Man - Cursed Strike/Stun
				useSkill(activeChar, 4708, false);
				break;
			case 1031: // Feline King - Slash
				useSkill(activeChar, 5135, false);
				break;
			case 1032: // Feline King - Spinning Slash
				useSkill(activeChar, 5136, false);
				break;
			case 1033: // Feline King - Grip of the Cat
				useSkill(activeChar, 5137, false);
				break;
			case 1034: // Magnus the Unicorn - Whiplash
				useSkill(activeChar, 5138, false);
				break;
			case 1035: // Magnus the Unicorn - Tridal Wave
				useSkill(activeChar, 5139, false);
				break;
			case 1036: // Spectral Lord - Corpse Kaboom
				useSkill(activeChar, 5142, false);
				break;
			case 1037: // Spectral Lord - Dicing Death
				useSkill(activeChar, 5141, false);
				break;
			case 1038: // Spectral Lord - Force Curse
				useSkill(activeChar, 5140, false);
				break;
			case 1039: // Swoop Cannon - Cannon Fodder
				if ((target != null) && target.isDoor())
				{
					useSkill(activeChar, 5110, false);
				}
				break;
			case 1040: // Swoop Cannon - Big Bang
				if ((target != null) && target.isDoor())
				{
					useSkill(activeChar, 5111, false);
				}
				break;
			case 1041: // Great Wolf - Bite Attack
				useSkill(activeChar, 5442, true);
				break;
			case 1042: // Great Wolf - Maul
				useSkill(activeChar, 5444, true);
				break;
			case 1043: // Great Wolf - Cry of the Wolf
				useSkill(activeChar, 5443, true);
				break;
			case 1044: // Great Wolf - Awakening
				useSkill(activeChar, 5445, true);
				break;
			case 1045: // Great Wolf - Howl
				useSkill(activeChar, 5584, true);
				break;
			case 1046: // Strider - Roar
				useSkill(activeChar, 5585, true);
				break;
			case 1047: // Divine Beast - Bite
				useSkill(activeChar, 5580, false);
				break;
			case 1048: // Divine Beast - Stun Attack
				useSkill(activeChar, 5581, false);
				break;
			case 1049: // Divine Beast - Fire Breath
				useSkill(activeChar, 5582, false);
				break;
			case 1050: // Divine Beast - Roar
				useSkill(activeChar, 5583, false);
				break;
			case 1051: // Feline Queen - Bless The Body
				useSkill(activeChar, 5638, false);
				break;
			case 1052: // Feline Queen - Bless The Soul
				useSkill(activeChar, 5639, false);
				break;
			case 1053: // Feline Queen - Haste
				useSkill(activeChar, 5640, false);
				break;
			case 1054: // Unicorn Seraphim - Acumen
				useSkill(activeChar, 5643, false);
				break;
			case 1055: // Unicorn Seraphim - Clarity
				useSkill(activeChar, 5647, false);
				break;
			case 1056: // Unicorn Seraphim - Empower
				useSkill(activeChar, 5648, false);
				break;
			case 1057: // Unicorn Seraphim - Wild Magic
				useSkill(activeChar, 5646, false);
				break;
			case 1058: // Nightshade - Death Whisper
				useSkill(activeChar, 5652, false);
				break;
			case 1059: // Nightshade - Focus
				useSkill(activeChar, 5653, false);
				break;
			case 1060: // Nightshade - Guidance
				useSkill(activeChar, 5654, false);
				break;
			case 1061: // Wild Beast Fighter, White Weasel - Death blow
				useSkill(activeChar, 5745, true);
				break;
			case 1062: // Wild Beast Fighter - Double attack
				useSkill(activeChar, 5746, true);
				break;
			case 1063: // Wild Beast Fighter - Spin attack
				useSkill(activeChar, 5747, true);
				break;
			case 1064: // Wild Beast Fighter - Meteor Shower
				useSkill(activeChar, 5748, true);
				break;
			case 1065: // Fox Shaman, Wild Beast Fighter, White Weasel, Fairy Princess - Awakening
				useSkill(activeChar, 5753, true);
				break;
			case 1066: // Fox Shaman, Spirit Shaman - Thunder Bolt
				useSkill(activeChar, 5749, true);
				break;
			case 1067: // Fox Shaman, Spirit Shaman - Flash
				useSkill(activeChar, 5750, true);
				break;
			case 1068: // Fox Shaman, Spirit Shaman - Lightning Wave
				useSkill(activeChar, 5751, true);
				break;
			case 1069: // Fox Shaman, Fairy Princess - Flare
				useSkill(activeChar, 5752, true);
				break;
			case 1070: // White Weasel, Fairy Princess, Improved Baby Buffalo, Improved Baby Kookaburra, Improved Baby Cougar, Spirit Shaman, Toy Knight, Turtle Ascetic - Buff control
				useSkill(activeChar, 5771, true);
				break;
			case 1071: // Tigress - Power Strike
				useSkill(activeChar, 5761, true);
				break;
			case 1072: // Toy Knight - Piercing attack
				useSkill(activeChar, 6046, true);
				break;
			case 1073: // Toy Knight - Whirlwind
				useSkill(activeChar, 6047, true);
				break;
			case 1074: // Toy Knight - Lance Smash
				useSkill(activeChar, 6048, true);
				break;
			case 1075: // Toy Knight - Battle Cry
				useSkill(activeChar, 6049, true);
				break;
			case 1076: // Turtle Ascetic - Power Smash
				useSkill(activeChar, 6050, true);
				break;
			case 1077: // Turtle Ascetic - Energy Burst
				useSkill(activeChar, 6051, true);
				break;
			case 1078: // Turtle Ascetic - Shockwave
				useSkill(activeChar, 6052, true);
				break;
			case 1079: // Turtle Ascetic - Howl
				useSkill(activeChar, 6053, true);
				break;
			case 1080: // Phoenix Rush
				useSkill(activeChar, 6041, false);
				break;
			case 1081: // Phoenix Cleanse
				useSkill(activeChar, 6042, false);
				break;
			case 1082: // Phoenix Flame Feather
				useSkill(activeChar, 6043, false);
				break;
			case 1083: // Phoenix Flame Beak
				useSkill(activeChar, 6044, false);
				break;
			case 1084: // Switch State
				break;
			case 1086: // Panther Cancel
				useSkill(activeChar, 6094, false);
				break;
			case 1087: // Panther Dark Claw
				useSkill(activeChar, 6095, false);
				break;
			case 1088: // Panther Fatal Claw
				useSkill(activeChar, 6096, false);
				break;
			case 1089: // Deinonychus - Tail Strike
				useSkill(activeChar, 6199, true);
				break;
			case 1090: // Guardian's Strider - Strider Bite
				useSkill(activeChar, 6205, true);
				break;
			case 1091: // Guardian's Strider - Strider Fear
				useSkill(activeChar, 6206, true);
				break;
			case 1092: // Guardian's Strider - Strider Dash
				useSkill(activeChar, 6207, true);
				break;
			case 1093: // Maguen - Maguen Strike
				useSkill(activeChar, 6618, true);
				break;
			case 1094: // Maguen - Maguen Wind Walk
				useSkill(activeChar, 6681, true);
				break;
			case 1095: // Elite Maguen - Maguen Power Strike
				useSkill(activeChar, 6619, true);
				break;
			case 1096: // Elite Maguen - Elite Maguen Wind Walk
				useSkill(activeChar, 6682, true);
				break;
			case 1097: // Maguen - Maguen Return
				useSkill(activeChar, 6683, true);
				break;
			case 1098: // Elite Maguen - Maguen Party Return
				useSkill(activeChar, 6684, true);
				break;
			case 1100: // All servitor move to
				activeChar.getServitors().values().forEach(s ->
				{
					if (validateSummon(activeChar, s, false))
					{
						if ((target != null) && (s != target) && !s.isMovementDisabled())
						{
							s.setFollowStatus(false);
							s.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, target.getLocation());
						}
					}
				});
				break;
			case 1101: // All servitor stop
				activeChar.getServitors().values().forEach(summon ->
				{
					if (validateSummon(activeChar, summon, false))
					{
						summon.cancelAction();
					}
				});
				break;
			case 1103: // seems to be passive mode
				activeChar.getServitors().values().forEach(summon -> ((SummonAI) summon.getAI()).setDefending(false));
				break;
			case 1104: // seems to be defend mode
				activeChar.getServitors().values().forEach(summon -> ((SummonAI) summon.getAI()).setDefending(true));
				break;
			case 1106: // Cute Bear - Bear Claw
				useServitorsSkill(activeChar, 11278);
				break;
			case 1107: // Cute Bear - Bear Tumbling
				useServitorsSkill(activeChar, 11279);
				break;
			case 1108: // Saber Tooth Cougar- Cougar Bite
				useServitorsSkill(activeChar, 11280);
				break;
			case 1109: // Saber Tooth Cougar - Cougar Pounce
				useServitorsSkill(activeChar, 11281);
				break;
			case 1110: // Grim Reaper - Reaper Touch
				useServitorsSkill(activeChar, 11282);
				break;
			case 1111: // Grim Reaper - Reaper Power
				useServitorsSkill(activeChar, 11283);
				break;
			case 1113: // Golden Lion - Lion Roar
				useSkill(activeChar, 10051, false);
				break;
			case 1114: // Golden Lion - Lion Claw
				useSkill(activeChar, 10052, false);
				break;
			case 1115: // Golden Lion - Lion Dash
				useSkill(activeChar, 10053, false);
				break;
			case 1116: // Golden Lion - Lion Flame
				useSkill(activeChar, 10054, false);
				break;
			case 1117: // Thunder Hawk - Thunder Flight
				useSkill(activeChar, 10794, false);
				break;
			case 1118: // Thunder Hawk - Thunder Purity
				useSkill(activeChar, 10795, false);
				break;
			case 1120: // Thunder Hawk - Thunder Feather Blast
				useSkill(activeChar, 10797, false);
				break;
			case 1121: // Thunder Hawk - Thunder Sharp Claw
				useSkill(activeChar, 10798, false);
				break;
			case 1122: // Tree of Life - Blessing of Tree
				useServitorsSkill(activeChar, 11806);
				break;
			case 1124: // Wynn Kai the Cat - Feline Aggression
				useServitorsSkill(activeChar, 11323);
				break;
			case 1125: // Wynn Kai the Cat - Feline Stun
				useServitorsSkill(activeChar, 11324);
				break;
			case 1126: // Wynn Feline King - Feline Bite
				useServitorsSkill(activeChar, 11325);
				break;
			case 1127: // Wynn Feline King - Feline Pounce
				useServitorsSkill(activeChar, 11326);
				break;
			case 1128: // Wynn Feline Queen - Feline Touch
				useServitorsSkill(activeChar, 11327);
				break;
			case 1129: // Wynn Feline Queen - Feline Power
				useServitorsSkill(activeChar, 11328);
				break;
			case 1130: // Wynn Merrow - Unicorn's Aggression
				useServitorsSkill(activeChar, 11332);
				break;
			case 1131: // Wynn Merrow - Unicorn's Stun
				useServitorsSkill(activeChar, 11333);
				break;
			case 1132: // Wynn Magnus - Unicorn's Bite
				useServitorsSkill(activeChar, 11334);
				break;
			case 1133: // Wynn Magnus - Unicorn's Pounce
				useServitorsSkill(activeChar, 11335);
				break;
			case 1134: // Wynn Seraphim - Unicorn's Touch
				useServitorsSkill(activeChar, 11336);
				break;
			case 1135: // Wynn Seraphim - Unicorn's Power
				useServitorsSkill(activeChar, 11337);
				break;
			case 1136: // Wynn Nightshade - Phantom Aggression
				useServitorsSkill(activeChar, 11341);
				break;
			case 1137: // Wynn Nightshade - Phantom Stun
				useServitorsSkill(activeChar, 11342);
				break;
			case 1138: // Wynn Spectral Lord - Phantom Bite
				useServitorsSkill(activeChar, 11343);
				break;
			case 1139: // Wynn Spectral Lord - Phantom Pounce
				useServitorsSkill(activeChar, 11344);
				break;
			case 1140: // Wynn Soulless - Phantom Touch
				useServitorsSkill(activeChar, 11345);
				break;
			case 1141: // Wynn Soulless - Phantom Power
				useServitorsSkill(activeChar, 11346);
				break;
			case 1142: // Blood Panther - Panther Roar
				useServitorsSkill(activeChar, 10087);
				break;
			case 1143: // Blood Panther - Panther Rush
				useServitorsSkill(activeChar, 10088);
				break;
			case 1144: // Commando Cat - Commando Jumping Attack
				useServitorsSkill(activeChar, 11375);
				break;
			case 1145: // Commando Cat - Commando Double Slash
				useServitorsSkill(activeChar, 11376);
				break;
			case 1146: // Witch Cat - Elemental Slam
				useServitorsSkill(activeChar, 11378);
				break;
			case 1147: // Witch Cat - Witch Cat Power
				useServitorsSkill(activeChar, 11377);
				break;
			case 1148: // Unicorn Lancer - Lancer Rush
				useServitorsSkill(activeChar, 11379);
				break;
			case 1149: // Unicorn Lancer - Power Stamp
				useServitorsSkill(activeChar, 11380);
				break;
			case 1150: // Unicorn Cherub - Multiple Icicles
				useServitorsSkill(activeChar, 11382);
				break;
			case 1151: // Unicorn Cherub - Cherub Power
				useServitorsSkill(activeChar, 11381);
				break;
			case 1152: // Dark Crusader - Phantom Sword Attack
				useServitorsSkill(activeChar, 11383);
				break;
			case 1153: // Dark Crusader - Phantom Blow
				useServitorsSkill(activeChar, 11384);
				break;
			case 1154: // Banshee Queen - Phantom Spike
				useServitorsSkill(activeChar, 11385);
				break;
			case 1155: // Banshee Queen - Phantom Crash
				useServitorsSkill(activeChar, 11386);
				break;
			case 5000: // Baby Rudolph - Reindeer Scratch
				useSkill(activeChar, 23155, true);
				break;
			case 5001: // Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum - Rosy Seduction
				useSkill(activeChar, 23167, true);
				break;
			case 5002: // Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum - Critical Seduction
				useSkill(activeChar, 23168, true);
				break;
			case 5003: // Hyum, Lapham, Hyum, Lapham - Thunder Bolt
				useSkill(activeChar, 5749, true);
				break;
			case 5004: // Hyum, Lapham, Hyum, Lapham - Flash
				useSkill(activeChar, 5750, true);
				break;
			case 5005: // Hyum, Lapham, Hyum, Lapham - Lightning Wave
				useSkill(activeChar, 5751, true);
				break;
			case 5006: // Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum, Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum - Buff Control
				useSkill(activeChar, 5771, true);
				break;
			case 5007: // Deseloph, Lilias, Deseloph, Lilias - Piercing Attack
				useSkill(activeChar, 6046, true);
				break;
			case 5008: // Deseloph, Lilias, Deseloph, Lilias - Spin Attack
				useSkill(activeChar, 6047, true);
				break;
			case 5009: // Deseloph, Lilias, Deseloph, Lilias - Smash
				useSkill(activeChar, 6048, true);
				break;
			case 5010: // Deseloph, Lilias, Deseloph, Lilias - Ignite
				useSkill(activeChar, 6049, true);
				break;
			case 5011: // Rekang, Mafum, Rekang, Mafum - Power Smash
				useSkill(activeChar, 6050, true);
				break;
			case 5012: // Rekang, Mafum, Rekang, Mafum - Energy Burst
				useSkill(activeChar, 6051, true);
				break;
			case 5013: // Rekang, Mafum, Rekang, Mafum - Shockwave
				useSkill(activeChar, 6052, true);
				break;
			case 5014: // Rekang, Mafum, Rekang, Mafum - Ignite
				useSkill(activeChar, 6053, true);
				break;
			case 5015: // Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum, Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum - Switch Stance
				useSkill(activeChar, 6054, true);
				break;
			// Social Packets
			default:
				_log.warn(activeChar.getName() + ": unhandled action type " + _actionId);
				break;
		}
	}
	
	/**
	 * Use the sit action.
	 * @param activeChar the player trying to sit
	 * @param target the target to sit, throne, bench or chair
	 * @return {@code true} if the player can sit, {@code false} otherwise
	 */
	protected boolean useSit(PlayerInstance activeChar, WorldObject target)
	{
		if (activeChar.getMountType() != MountType.NONE)
		{
			return false;
		}
		
		if (!activeChar.isSitting() && (target instanceof L2StaticObjectInstance) && (((L2StaticObjectInstance) target).getType() == 1) && activeChar.isInsideRadius(target, L2StaticObjectInstance.INTERACTION_DISTANCE, false, false))
		{
			final ChairSit cs = new ChairSit(activeChar, target.getId());
			activeChar.sendPacket(cs);
			activeChar.sitDown();
			activeChar.broadcastPacket(cs);
			return true;
		}
		
		if (activeChar.isFakeDeath())
		{
			activeChar.stopEffects(L2EffectType.FAKE_DEATH);
		}
		else if (activeChar.isSitting())
		{
			activeChar.standUp();
		}
		else
		{
			activeChar.sitDown();
		}
		return true;
	}
	
	/**
	 * Cast a skill for active summon.<br>
	 * Target is specified as a parameter but can be overwrited or ignored depending on skill type.
	 * @param player TODO
	 * @param skillId the skill Id to be casted by the summon
	 * @param target the target to cast the skill on, overwritten or ignored depending on skill type
	 * @param pet if {@code true} it'll validate a pet, if {@code false} it will validate a servitor
	 */
	private void useSkill(PlayerInstance player, int skillId, WorldObject target, boolean pet)
	{
		if (pet)
		{
			final Summon summon = player.getPet();
			if (!validateSummon(player, summon, pet))
			{
				return;
			}
			
			if ((summon.getLevel() - player.getLevel()) > 20)
			{
				player.sendPacket(SystemMessageId.YOUR_PET_IS_TOO_HIGH_LEVEL_TO_CONTROL);
				return;
			}
			
			final int lvl = PetDataTable.getInstance().getPetData(summon.getId()).getAvailableLevel(skillId, summon.getLevel());
			
			if (lvl > 0)
			{
				summon.setTarget(target);
				summon.useMagic(SkillData.getInstance().getSkill(skillId, lvl), _ctrlPressed, _shiftPressed);
			}
			
			if (skillId == SWITCH_STANCE_ID)
			{
				summon.switchMode();
			}
		}
		else
		{
			final Summon servitor = player.getAnyServitor();
			if (!validateSummon(player, servitor, pet))
			{
				return;
			}
			
			final int lvl = SummonSkillsTable.getInstance().getAvailableLevel(servitor, skillId);
			
			if (lvl > 0)
			{
				servitor.setTarget(target);
				servitor.useMagic(SkillData.getInstance().getSkill(skillId, lvl), _ctrlPressed, _shiftPressed);
			}
		}
	}
	
	/**
	 * Cast a skill for active summon.<br>
	 * Target is retrieved from owner's target, then validated by overloaded method useSkill(int, L2Character).
	 * @param player TODO
	 * @param skillId the skill Id to use
	 * @param pet if {@code true} it'll validate a pet, if {@code false} it will validate a servitor
	 */
	private void useSkill(PlayerInstance player, int skillId, boolean pet)
	{
		useSkill(player, skillId, player.getTarget(), pet);
	}
	
	/**
	 * Cast a skill for all active summon.<br>
	 * Target is retrieved from owner's target
	 * @param player TODO
	 * @param skillId the skill Id to use
	 */
	private void useServitorsSkill(PlayerInstance player, int skillId)
	{
		player.getServitors().values().forEach(servitor ->
		{
			if (!validateSummon(player, servitor, false))
			{
				return;
			}
			
			final int lvl = SummonSkillsTable.getInstance().getAvailableLevel(servitor, skillId);
			
			if (lvl > 0)
			{
				servitor.setTarget(player.getTarget());
				servitor.useMagic(SkillData.getInstance().getSkill(skillId, lvl), _ctrlPressed, _shiftPressed);
			}
		});
	}
	
	/**
	 * Validates the given summon and sends a system message to the master.
	 * @param player TODO
	 * @param summon the summon to validate
	 * @param checkPet if {@code true} it'll validate a pet, if {@code false} it will validate a servitor
	 * @return {@code true} if the summon is not null and whether is a pet or a servitor depending on {@code checkPet} value, {@code false} otherwise
	 */
	private boolean validateSummon(PlayerInstance player, Summon summon, boolean checkPet)
	{
		if ((summon != null) && ((checkPet && summon.isPet()) || summon.isServitor()))
		{
			if (summon.isPet() && ((L2PetInstance) summon).isUncontrollable())
			{
				player.sendPacket(SystemMessageId.WHEN_YOUR_PET_S_HUNGER_GAUGE_IS_AT_0_YOU_CANNOT_USE_YOUR_PET);
				return false;
			}
			if (summon.isBetrayed())
			{
				player.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
				return false;
			}
			return true;
		}
		
		if (checkPet)
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_PET);
		}
		else
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_SERVITOR);
		}
		return false;
	}
	
	/**
	 * @param skillId
	 * @return the actionId that calls the given skillId <br>
	 *         TODO This shouldn't be here, nor handled like this... create a better handling.
	 */
	public static int getActionId(int skillId)
	{
		switch (skillId)
		{//@formatter:off
			case 4079: return 1000; // Siege Golem - Siege Hammer
			case 4710: return 1003; // Wind Hatchling/Strider - Wild Stun
			case 4711: return 1004; // Wind Hatchling/Strider - Wild Defense
			case 4712: return 1005; // Star Hatchling/Strider - Bright Burst
			case 4713: return 1006; // Star Hatchling/Strider - Bright Heal
			case 4699: return 1007; // Cat Queen - Blessing of Queen
			case 4700: return 1008; // Cat Queen - Gift of Queen
			case 4701: return 1009; // Cat Queen - Cure of Queen
			case 4702: return 1010; // Unicorn Seraphim - Blessing of Seraphim
			case 4703: return 1011; // Unicorn Seraphim - Gift of Seraphim
			case 4704: return 1012; // Unicorn Seraphim - Cure of Seraphim
			case 4705: return 1013; // Nightshade - Curse of Shade
			case 4706: return 1014; // Nightshade - Mass Curse of Shade
			case 4707: return 1015; // Nightshade - Shade Sacrifice
			case 4709: return 1016; // Cursed Man - Cursed Blow
			case 4708: return 1017; // Cursed Man - Cursed Strike/Stun
			case 5135: return 1031; // Feline King - Slash
			case 5136: return 1032; // Feline King - Spinning Slash
			case 5137: return 1033; // Feline King - Grip of the Cat
			case 5138: return 1034; // Magnus the Unicorn - Whiplash
			case 5139: return 1035; // Magnus the Unicorn - Tridal Wave
			case 5142: return 1036; // Spectral Lord - Corpse Kaboom
			case 5141: return 1037; // Spectral Lord - Dicing Death
			case 5140: return 1038; // Spectral Lord - Force Curse
			case 5110: return 1039; // Swoop Cannon - Cannon Fodder
			case 5111: return 1040; // Swoop Cannon - Big Bang
			case 5442: return 1041; // Great Wolf - Bite Attack
			case 5444: return 1042; // Great Wolf - Maul
			case 5443: return 1043; // Great Wolf - Cry of the Wolf
			case 5445: return 1044; // Great Wolf - Awakening
			case 5584: return 1045; // Great Wolf - Howl
			case 5585: return 1046; // Strider - Roar
			case 5580: return 1047; // Divine Beast - Bite
			case 5581: return 1048; // Divine Beast - Stun Attack
			case 5582: return 1049; // Divine Beast - Fire Breath
			case 5583: return 1050; // Divine Beast - Roar
			case 5638: return 1051; // Feline Queen - Bless The Body
			case 5639: return 1052; // Feline Queen - Bless The Soul
			case 5640: return 1053; // Feline Queen - Haste
			case 5643: return 1054; // Unicorn Seraphim - Acumen
			case 5647: return 1055; // Unicorn Seraphim - Clarity
			case 5648: return 1056; // Unicorn Seraphim - Empower
			case 5646: return 1057; // Unicorn Seraphim - Wild Magic
			case 5652: return 1058; // Nightshade - Death Whisper
			case 5653: return 1059; // Nightshade - Focus
			case 5654: return 1060; // Nightshade - Guidance
			case 5745: return 1061; // Wild Beast Fighter, White Weasel - Death blow
			case 5746: return 1062; // Wild Beast Fighter - Double attack
			case 5747: return 1063; // Wild Beast Fighter - Spin attack
			case 5748: return 1064; // Wild Beast Fighter - Meteor Shower
			case 5753: return 1065; // Fox Shaman, Wild Beast Fighter, White Weasel, Fairy Princess - Awakening
			case 5749: return 1066; // Fox Shaman, Spirit Shaman - Thunder Bolt
			case 5750: return 1067; // Fox Shaman, Spirit Shaman - Flash
			case 5751: return 1068; // Fox Shaman, Spirit Shaman - Lightning Wave
			case 5752: return 1068; // Fox Shaman, Fairy Princess - Flare
			case 5771: return 1070; // White Weasel, Fairy Princess, Improved Baby Buffalo, Improved Baby Kookaburra, Improved Baby Cougar, Spirit Shaman, Toy Knight, Turtle Ascetic - Buff control
			case 5761: return 1071; // Tigress - Power Strike
			case 6046: return 1072; // Toy Knight - Piercing attack
			case 6047: return 1073; // Toy Knight - Whirlwind
			case 6048: return 1074; // Toy Knight - Lance Smash
			case 6049: return 1075; // Toy Knight - Battle Cry
			case 6050: return 1076; // Turtle Ascetic - Power Smash
			case 6051: return 1077; // Turtle Ascetic - Energy Burst
			case 6052: return 1078; // Turtle Ascetic - Shockwave
			case 6053: return 1079; // Turtle Ascetic - Howl
			case 6041: return 1080; // Phoenix Rush
			case 6042: return 1081; // Phoenix Cleanse
			case 6043: return 1082; // Phoenix Flame Feather
			case 6044: return 1083; // Phoenix Flame Beak
			case 6094: return 1086; // Panther Cancel
			case 6095: return 1087; // Panther Dark Claw
			case 6096: return 1088; // Panther Fatal Claw
			case 6199: return 1089; // Deinonychus - Tail Strike
			case 6205: return 1090; // Guardian's Strider - Strider Bite
			case 6206: return 1091; // Guardian's Strider - Strider Fear
			case 6207: return 1092; // Guardian's Strider - Strider Dash
			case 6618: return 1093; // Maguen - Maguen Strike
			case 6681: return 1094; // Maguen - Maguen Wind Walk
			case 6619: return 1095; // Elite Maguen - Maguen Power Strike
			case 6682: return 1096; // Elite Maguen - Elite Maguen Wind Walk
			case 6683: return 1097; // Maguen - Maguen Return
			case 6684: return 1098; // Elite Maguen - Maguen Party Return
			case 11278: return 1106; // Cute Bear - Bear Claw
			case 11279: return 1107; // Cute Bear - Bear Tumbling
			case 11280: return 1108; // Saber Tooth Cougar- Cougar Bite
			case 11281: return 1109; // Saber Tooth Cougar - Cougar Pounce
			case 11282: return 1110; // Grim Reaper - Reaper Touch
			case 11283: return 1111; // Grim Reaper - Reaper Power
			case 10051: return 1113; // Golden Lion - Lion Roar
			case 10052: return 1114; // Golden Lion - Lion Claw
			case 10053: return 1115; // Golden Lion - Lion Dash
			case 10054: return 1116; // Golden Lion - Lion Flame
			case 10794: return 1117; // Thunder Hawk - Thunder Flight
			case 10795: return 1118; // Thunder Hawk - Thunder Purity
			case 10797: return 1120; // Thunder Hawk - Thunder Feather Blast
			case 10798: return 1121; // Thunder Hawk - Thunder Sharp Claw
			case 11806: return 1122; // Tree of Life - Blessing of Tree
			case 11323: return 1124; // Wynn Kai the Cat - Feline Aggression
			case 11324: return 1125; // Wynn Kai the Cat - Feline Stun
			case 11325: return 1126; // Wynn Feline King - Feline Bite
			case 11326: return 1127; // Wynn Feline King - Feline Pounce
			case 11327: return 1128; // Wynn Feline Queen - Feline Touch
			case 11328: return 1129; // Wynn Feline Queen - Feline Power
			case 11332: return 1130; // Wynn Merrow - Unicorn's Aggression
			case 11333: return 1131; // Wynn Merrow - Unicorn's Stun
			case 11334: return 1132; // Wynn Magnus - Unicorn's Bite
			case 11335: return 1133; // Wynn Magnus - Unicorn's Pounce
			case 11336: return 1134; // Wynn Seraphim - Unicorn's Touch
			case 11337: return 1135; // Wynn Seraphim - Unicorn's Power
			case 11341: return 1136; // Wynn Nightshade - Phantom Aggression
			case 11342: return 1137; // Wynn Nightshade - Phantom Stun
			case 11343: return 1138; // Wynn Spectral Lord - Phantom Bite
			case 11344: return 1139; // Wynn Spectral Lord - Phantom Pounce
			case 11345: return 1140; // Wynn Soulless - Phantom Touch
			case 11346: return 1141; // Wynn Soulless - Phantom Power
			case 10087: return 1142; // Blood Panther - Panther Roar
			case 10088: return 1143; // Blood Panther - Panther Rush
			case 11375: return 1144; // Commando Cat - Commando Jumping Attack
			case 11376: return 1145; // Commando Cat - Commando Double Slash
			case 11378: return 1146; // Witch Cat - Elemental Slam
			case 11377: return 1147; // Witch Cat - Witch Cat Power
			case 11379: return 1148; // Unicorn Lancer - Lancer Rush
			case 11380: return 1149; // Unicorn Lancer - Power Stamp
			case 11382: return 1150; // Unicorn Cherub - Multiple Icicles
			case 11381: return 1151; // Unicorn Cherub - Cherub Power
			case 11383: return 1152; // Dark Crusader - Phantom Sword Attack
			case 11384: return 1153; // Dark Crusader - Phantom Blow
			case 11385: return 1154; // Banshee Queen - Phantom Spike
			case 11386: return 1155; // Banshee Queen - Phantom Crash
			case 23155: return 5000; // Baby Rudolph - Reindeer Scratch
			case 23167: return 5001; // Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum - Rosy Seduction
			case 23168: return 5002; // Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum - Critical Seduction
			case 6054: return 5015; // Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum, Deseloph, Hyum, Rekang, Lilias, Lapham, Mafum - Switch Stance
			default: return 0;
		}//@formatter:on
	}
}
