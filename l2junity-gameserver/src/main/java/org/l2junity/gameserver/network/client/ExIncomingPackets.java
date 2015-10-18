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
package org.l2junity.gameserver.network.client;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.l2junity.gameserver.network.client.recv.*;
import org.l2junity.gameserver.network.client.recv.ability.RequestAbilityList;
import org.l2junity.gameserver.network.client.recv.ability.RequestAbilityWndClose;
import org.l2junity.gameserver.network.client.recv.ability.RequestAbilityWndOpen;
import org.l2junity.gameserver.network.client.recv.ability.RequestAcquireAbilityList;
import org.l2junity.gameserver.network.client.recv.ability.RequestChangeAbilityPoint;
import org.l2junity.gameserver.network.client.recv.ability.RequestResetAbilityPoint;
import org.l2junity.gameserver.network.client.recv.adenadistribution.RequestDivideAdena;
import org.l2junity.gameserver.network.client.recv.adenadistribution.RequestDivideAdenaCancel;
import org.l2junity.gameserver.network.client.recv.adenadistribution.RequestDivideAdenaStart;
import org.l2junity.gameserver.network.client.recv.alchemy.RequestAlchemyConversion;
import org.l2junity.gameserver.network.client.recv.alchemy.RequestAlchemyTryMixCube;
import org.l2junity.gameserver.network.client.recv.appearance.RequestExCancelShape_Shifting_Item;
import org.l2junity.gameserver.network.client.recv.appearance.RequestExTryToPutShapeShiftingEnchantSupportItem;
import org.l2junity.gameserver.network.client.recv.appearance.RequestExTryToPutShapeShiftingTargetItem;
import org.l2junity.gameserver.network.client.recv.appearance.RequestShapeShiftingItem;
import org.l2junity.gameserver.network.client.recv.ceremonyofchaos.RequestCancelCuriousHouse;
import org.l2junity.gameserver.network.client.recv.ceremonyofchaos.RequestCuriousHouseHtml;
import org.l2junity.gameserver.network.client.recv.ceremonyofchaos.RequestJoinCuriousHouse;
import org.l2junity.gameserver.network.client.recv.commission.RequestCommissionBuyInfo;
import org.l2junity.gameserver.network.client.recv.commission.RequestCommissionBuyItem;
import org.l2junity.gameserver.network.client.recv.commission.RequestCommissionCancel;
import org.l2junity.gameserver.network.client.recv.commission.RequestCommissionDelete;
import org.l2junity.gameserver.network.client.recv.commission.RequestCommissionInfo;
import org.l2junity.gameserver.network.client.recv.commission.RequestCommissionList;
import org.l2junity.gameserver.network.client.recv.commission.RequestCommissionRegister;
import org.l2junity.gameserver.network.client.recv.commission.RequestCommissionRegisteredItem;
import org.l2junity.gameserver.network.client.recv.commission.RequestCommissionRegistrableItemList;
import org.l2junity.gameserver.network.client.recv.compound.RequestNewEnchantClose;
import org.l2junity.gameserver.network.client.recv.compound.RequestNewEnchantPushOne;
import org.l2junity.gameserver.network.client.recv.compound.RequestNewEnchantPushTwo;
import org.l2junity.gameserver.network.client.recv.compound.RequestNewEnchantRemoveOne;
import org.l2junity.gameserver.network.client.recv.compound.RequestNewEnchantRemoveTwo;
import org.l2junity.gameserver.network.client.recv.compound.RequestNewEnchantTry;
import org.l2junity.gameserver.network.client.recv.crystalization.RequestCrystallizeEstimate;
import org.l2junity.gameserver.network.client.recv.crystalization.RequestCrystallizeItemCancel;
import org.l2junity.gameserver.network.client.recv.friend.RequestFriendDetailInfo;
import org.l2junity.gameserver.network.client.recv.mentoring.ConfirmMenteeAdd;
import org.l2junity.gameserver.network.client.recv.mentoring.RequestMenteeAdd;
import org.l2junity.gameserver.network.client.recv.mentoring.RequestMenteeWaitingList;
import org.l2junity.gameserver.network.client.recv.mentoring.RequestMentorCancel;
import org.l2junity.gameserver.network.client.recv.mentoring.RequestMentorList;
import org.l2junity.gameserver.network.client.recv.pledgebonus.RequestPledgeBonusOpen;
import org.l2junity.gameserver.network.client.recv.pledgebonus.RequestPledgeBonusReward;
import org.l2junity.gameserver.network.client.recv.pledgebonus.RequestPledgeBonusRewardList;
import org.l2junity.gameserver.network.client.recv.primeshop.RequestBRBuyProduct;
import org.l2junity.gameserver.network.client.recv.primeshop.RequestBRGamePoint;
import org.l2junity.gameserver.network.client.recv.primeshop.RequestBRProductInfo;
import org.l2junity.gameserver.network.client.recv.primeshop.RequestBRProductList;
import org.l2junity.gameserver.network.client.recv.primeshop.RequestBRRecentProductList;
import org.l2junity.gameserver.network.client.recv.sayune.RequestFlyMove;
import org.l2junity.gameserver.network.client.recv.sayune.RequestFlyMoveStart;
import org.l2junity.gameserver.network.client.recv.shuttle.CannotMoveAnymoreInShuttle;
import org.l2junity.gameserver.network.client.recv.shuttle.MoveToLocationInShuttle;
import org.l2junity.gameserver.network.client.recv.shuttle.RequestShuttleGetOff;
import org.l2junity.gameserver.network.client.recv.shuttle.RequestShuttleGetOn;
import org.l2junity.network.IConnectionState;
import org.l2junity.network.IIncomingPacket;
import org.l2junity.network.IIncomingPackets;

/**
 * @author Sdw
 */
public enum ExIncomingPackets implements IIncomingPackets<L2GameClient>
{
	REQUEST_GOTO_LOBBY(0x33, RequestGotoLobby::new, ConnectionState.AUTHENTICATED),
	REQUEST_EX_2ND_PASSWORD_CHECK(0xA6, RequestEx2ndPasswordCheck::new, ConnectionState.AUTHENTICATED),
	REQUEST_EX_2ND_PASSWORD_VERIFY(0xA7, RequestEx2ndPasswordVerify::new, ConnectionState.AUTHENTICATED),
	REQUEST_EX_2ND_PASSWORD_REQ(0xA8, RequestEx2ndPasswordReq::new, ConnectionState.AUTHENTICATED),
	REQUEST_CHARACTER_NAME_CREATABLE(0xA9, RequestCharacterNameCreatable::new, ConnectionState.AUTHENTICATED),
	REQUEST_MANOR_LIST(0x01, RequestManorList::new, ConnectionState.IN_GAME),
	REQUEST_PROCEDURE_CROP_LIST(0x02, RequestProcureCropList::new, ConnectionState.IN_GAME),
	REQUEST_SET_SEED(0x03, RequestSetSeed::new, ConnectionState.IN_GAME),
	REQUEST_SET_CROP(0x04, RequestSetCrop::new, ConnectionState.IN_GAME),
	REQUEST_WRITE_HERO_WORDS(0x05, RequestWriteHeroWords::new, ConnectionState.IN_GAME),
	REQUEST_EX_ASK_JOIN_MPCC(0x06, RequestExAskJoinMPCC::new, ConnectionState.IN_GAME),
	REQUEST_EX_ACCEPT_JOIN_MPCC(0x07, RequestExAcceptJoinMPCC::new, ConnectionState.IN_GAME),
	REQUEST_EX_OUST_FROM_MPCC(0x08, RequestExOustFromMPCC::new, ConnectionState.IN_GAME),
	REQUEST_OUST_FROM_PARTY_ROOM(0x09, RequestOustFromPartyRoom::new, ConnectionState.IN_GAME),
	REQUEST_DISMISS_PARTY_ROOM(0x0A, RequestDismissPartyRoom::new, ConnectionState.IN_GAME),
	REQUEST_WITHDRAW_PARTY_ROOM(0x0B, RequestWithdrawPartyRoom::new, ConnectionState.IN_GAME),
	REQUEST_CHANGE_PARTY_LEADER(0x0C, RequestChangePartyLeader::new, ConnectionState.IN_GAME),
	REQUEST_AUTO_SOULSHOT(0x0D, RequestAutoSoulShot::new, ConnectionState.IN_GAME),
	REQUEST_EX_ENCHANT_SKILL_INFO(0x0E, RequestExEnchantSkillInfo::new, ConnectionState.IN_GAME),
	REQUEST_EX_ENCHANT_SKILL(0x0F, RequestExEnchantSkill::new, ConnectionState.IN_GAME),
	REQUEST_EX_PLEDGE_CREST_LARGE(0x10, RequestExPledgeCrestLarge::new, ConnectionState.IN_GAME),
	REQUEST_EX_SET_PLEDGE_CREST_LARGE(0x11, RequestExSetPledgeCrestLarge::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_SET_ACADEMY_MASTER(0x12, RequestPledgeSetAcademyMaster::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_POWER_GRADE_LIST(0x13, RequestPledgePowerGradeList::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_MEMBER_POWER_INFO(0x14, RequestPledgeMemberPowerInfo::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_SET_MEMBER_POWER_GRADE(0x15, RequestPledgeSetMemberPowerGrade::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_MEMBER_INFO(0x16, RequestPledgeMemberInfo::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_WAR_LIST(0x17, RequestPledgeWarList::new, ConnectionState.IN_GAME),
	REQUEST_EX_FISH_RANKING(0x18, RequestExFishRanking::new, ConnectionState.IN_GAME),
	REQUEST_PCCAFE_COUPON_USE(0x19, RequestPCCafeCouponUse::new, ConnectionState.IN_GAME),
	REQUEST_SERVER_LOGIN(0x1A, null, ConnectionState.IN_GAME),
	REQUEST_DUEL_START(0x1B, RequestDuelStart::new, ConnectionState.IN_GAME),
	REQUEST_DUAL_ANSWER_START(0x1C, RequestDuelAnswerStart::new, ConnectionState.IN_GAME),
	REQUEST_EX_SET_TUTORIAL(0x1D, null, ConnectionState.IN_GAME),
	REQUEST_EX_RQ_ITEM_LINK(0x1E, RequestExRqItemLink::new, ConnectionState.IN_GAME),
	CANNOT_MOVE_ANYMORE_AIR_SHIP(0x1F, null, ConnectionState.IN_GAME),
	MOVE_TO_LOCATION_IN_AIR_SHIP(0x20, MoveToLocationInAirShip::new, ConnectionState.IN_GAME),
	REQUEST_KEY_MAPPING(0x21, RequestKeyMapping::new, ConnectionState.IN_GAME),
	REQUEST_SAVE_KEY_MAPPING(0x22, RequestSaveKeyMapping::new, ConnectionState.IN_GAME),
	REQUEST_EX_REMOVE_ITEM_ATTRIBUTE(0x23, RequestExRemoveItemAttribute::new, ConnectionState.IN_GAME),
	REQUEST_SAVE_INVENTORY_ORDER(0x24, RequestSaveInventoryOrder::new, ConnectionState.IN_GAME),
	REQUEST_EXIT_PARTY_MATCHING_WAITING_ROOM(0x25, RequestExitPartyMatchingWaitingRoom::new, ConnectionState.IN_GAME),
	REQUEST_CONFIRM_TARGET_ITEM(0x26, RequestConfirmTargetItem::new, ConnectionState.IN_GAME),
	REQUEST_CONFIRM_REFINER_ITEM(0x27, RequestConfirmRefinerItem::new, ConnectionState.IN_GAME),
	REQUEST_CONFIRM_GEMSTONE(0x28, RequestConfirmGemStone::new, ConnectionState.IN_GAME),
	REQUEST_OLYMPIAD_OBSERVER_END(0x29, RequestOlympiadObserverEnd::new, ConnectionState.IN_GAME),
	REQUEST_CURSED_WEAPON_LIST(0x2A, RequestCursedWeaponList::new, ConnectionState.IN_GAME),
	REQUEST_CURSED_WEAPON_LOCATION(0x2B, RequestCursedWeaponLocation::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_REORGANIZE_MEMBER(0x2C, RequestPledgeReorganizeMember::new, ConnectionState.IN_GAME),
	REQUEST_EX_MPCC_SHOW_PARTY_MEMBERS_INFO(0x2D, RequestExMPCCShowPartyMembersInfo::new, ConnectionState.IN_GAME),
	REQUEST_OLYMPIAD_MATCH_LIST(0x2E, RequestOlympiadMatchList::new, ConnectionState.IN_GAME),
	REQUEST_ASK_JOIN_PARTY_ROOM(0x2F, RequestAskJoinPartyRoom::new, ConnectionState.IN_GAME),
	ANSWER_JOIN_PARTY_ROOM(0x30, AnswerJoinPartyRoom::new, ConnectionState.IN_GAME),
	REQUEST_LIST_PARTY_MATCHING_WAITING_ROOM(0x31, RequestListPartyMatchingWaitingRoom::new, ConnectionState.IN_GAME),
	REQUEST_EX_ENCHANT_ITEM_ATTRIBUTE(0x32, RequestExEnchantItemAttribute::new, ConnectionState.IN_GAME),
	MOVE_TO_LOCATION_AIR_SHIP(0x35, MoveToLocationAirShip::new, ConnectionState.IN_GAME),
	REQUEST_BID_ITEM_AUCTION(0x36, RequestBidItemAuction::new, ConnectionState.IN_GAME),
	REQUEST_INFO_ITEM_AUCTION(0x37, RequestInfoItemAuction::new, ConnectionState.IN_GAME),
	REQUEST_EX_CHANGE_NAME(0x38, RequestExChangeName::new, ConnectionState.IN_GAME),
	REQUEST_ALL_CASTLE_INFO(0x39, RequestAllCastleInfo::new, ConnectionState.IN_GAME),
	REQUEST_ALL_FORTRESS_INFO(0x3A, RequestAllFortressInfo::new, ConnectionState.IN_GAME),
	REQUEST_ALL_AGIT_INGO(0x3B, RequestAllAgitInfo::new, ConnectionState.IN_GAME),
	REQUEST_FORTRESS_SIEGE_INFO(0x3C, RequestFortressSiegeInfo::new, ConnectionState.IN_GAME),
	REQUEST_GET_BOSS_RECORD(0x3D, RequestGetBossRecord::new, ConnectionState.IN_GAME),
	REQUEST_REFINE(0x3E, RequestRefine::new, ConnectionState.IN_GAME),
	REQUEST_CONFIRM_CANCEL_ITEM(0x3F, RequestConfirmCancelItem::new, ConnectionState.IN_GAME),
	REQUEST_REFINE_CANCEL(0x40, RequestRefineCancel::new, ConnectionState.IN_GAME),
	REQUEST_EX_MAGIC_SKILL_USE_GROUND(0x41, RequestExMagicSkillUseGround::new, ConnectionState.IN_GAME),
	REQUEST_DUEL_SURRENDER(0x42, RequestDuelSurrender::new, ConnectionState.IN_GAME),
	REQUEST_EX_ENCHANT_SKILL_INFO_DETAIL(0x43, RequestExEnchantSkillInfoDetail::new, ConnectionState.IN_GAME),
	REQUEST_FORTRESS_MAP_INFO(0x45, RequestFortressMapInfo::new, ConnectionState.IN_GAME),
	REQUEST_PVP_MATCH_RECORD(0x46, RequestPVPMatchRecord::new, ConnectionState.IN_GAME),
	SET_PRIVATE_STORE_WHOLE_MSG(0x47, SetPrivateStoreWholeMsg::new, ConnectionState.IN_GAME),
	REQUEST_DISPEL(0x48, RequestDispel::new, ConnectionState.IN_GAME),
	REQUEST_EX_TRY_TO_PUT_ENCHANT_TARGET_ITEM(0x49, RequestExTryToPutEnchantTargetItem::new, ConnectionState.IN_GAME),
	REQUEST_EX_TRY_TO_PUT_ENCHANT_SUPPORT_ITEM(0x4A, RequestExTryToPutEnchantSupportItem::new, ConnectionState.IN_GAME),
	REQUEST_EX_CANCEL_ENCHANT_ITEM(0x4B, RequestExCancelEnchantItem::new, ConnectionState.IN_GAME),
	REQUEST_CHANGE_NICKNAME_COLOR(0x4C, RequestChangeNicknameColor::new, ConnectionState.IN_GAME),
	REQUEST_RESET_NICKNAME(0x4D, RequestResetNickname::new, ConnectionState.IN_GAME),
	EX_BOOKMARK_PACKET(0x4E, ExBookmarkPacket::new, ConnectionState.IN_GAME),
	REQUEST_WITHDRAW_PREMIUM_ITEM(0x4F, RequestWithDrawPremiumItem::new, ConnectionState.IN_GAME),
	REQUEST_EX_JUMP(0x50, null, ConnectionState.IN_GAME),
	REQUEST_EX_START_SHOW_CRATAE_CUBE_RANK(0x51, null, ConnectionState.IN_GAME),
	REQUEST_EX_STOP_SHOW_CRATAE_CUBE_RANK(0x52, null, ConnectionState.IN_GAME),
	NOTIFY_START_MINI_GAME(0x53, null, ConnectionState.IN_GAME),
	REQUEST_EX_JOIN_DOMINION_WAR(0x54, null, ConnectionState.IN_GAME),
	REQUEST_EX_DOMINION_INFO(0x55, null, ConnectionState.IN_GAME),
	REQUEST_EX_CLEFT_ENTER(0x56, null, ConnectionState.IN_GAME),
	REQUEST_EX_CUBE_GAME_CHANGE_TEAM(0x57, RequestExCubeGameChangeTeam::new, ConnectionState.IN_GAME),
	END_SCENE_PLAYER(0x58, EndScenePlayer::new, ConnectionState.IN_GAME),
	REQUEST_EX_CUBE_GAME_READY_ANSWER(0x59, RequestExCubeGameReadyAnswer::new, ConnectionState.IN_GAME),
	REQUEST_EX_LIST_MPCC_WAITING(0x5A, RequestExListMpccWaiting::new, ConnectionState.IN_GAME),
	REQUEST_EX_MANAGE_MPCC_ROOM(0x5B, RequestExManageMpccRoom::new, ConnectionState.IN_GAME),
	REQUEST_EX_JOIN_MPCC_ROOM(0x5C, RequestExJoinMpccRoom::new, ConnectionState.IN_GAME),
	REQUEST_EX_OUST_FROM_MPCC_ROOM(0x5D, RequestExOustFromMpccRoom::new, ConnectionState.IN_GAME),
	REQUEST_EX_DISMISS_MPCC_ROOM(0x5E, RequestExDismissMpccRoom::new, ConnectionState.IN_GAME),
	REQUEST_EX_WITHDRAW_MPCC_ROOM(0x5F, RequestExWithdrawMpccRoom::new, ConnectionState.IN_GAME),
	REQUEST_SEED_PHASE(0x60, RequestSeedPhase::new, ConnectionState.IN_GAME),
	REQUEST_EX_MPCC_PARTYMASTER_LIST(0x61, RequestExMpccPartymasterList::new, ConnectionState.IN_GAME),
	REQUEST_POST_ITEM_LIST(0x62, RequestPostItemList::new, ConnectionState.IN_GAME),
	REQUEST_SEND_POST(0x63, RequestSendPost::new, ConnectionState.IN_GAME),
	REQUEST_RECEIVED_POST_LIST(0x64, RequestReceivedPostList::new, ConnectionState.IN_GAME),
	REQUEST_DELETE_RECEIVED_POST(0x65, RequestDeleteReceivedPost::new, ConnectionState.IN_GAME),
	REQUEST_RECEIVED_POST(0x66, RequestReceivedPost::new, ConnectionState.IN_GAME),
	REQUEST_POST_ATTACHMENT(0x67, RequestPostAttachment::new, ConnectionState.IN_GAME),
	REQUEST_REJECT_POST_ATTACHMENT(0x68, RequestRejectPostAttachment::new, ConnectionState.IN_GAME),
	REQUEST_SENT_POST_LIST(0x69, RequestSentPostList::new, ConnectionState.IN_GAME),
	REQUEST_DELETE_SENT_POST(0x6A, RequestDeleteSentPost::new, ConnectionState.IN_GAME),
	REQUEST_SENT_POST(0x6B, RequestSentPost::new, ConnectionState.IN_GAME),
	REQUEST_CANCEL_POST_ATTACHMENT(0x6C, RequestCancelPostAttachment::new, ConnectionState.IN_GAME),
	REQUEST_SHOW_NEW_USER_PETITION(0x6D, null, ConnectionState.IN_GAME),
	REQUEST_SHOW_STEP_TWO(0x6E, null, ConnectionState.IN_GAME),
	REQUEST_SHOW_STEP_THREE(0x6F, null, ConnectionState.IN_GAME),
	EX_CONNECT_TO_RAID_SERVER(0x70, null, ConnectionState.IN_GAME),
	EX_RETURN_FROM_RAID_SERVER(0x71, null, ConnectionState.IN_GAME),
	REQUEST_REFUND_ITEM(0x72, RequestRefundItem::new, ConnectionState.IN_GAME),
	REQUEST_BUI_SELL_UI_CLOSE(0x73, RequestBuySellUIClose::new, ConnectionState.IN_GAME),
	REQUEST_EX_EVENT_MATCH_OBSERVER_END(0x74, null, ConnectionState.IN_GAME),
	REQUEST_PARTY_LOOT_MODIFICATION(0x75, RequestPartyLootModification::new, ConnectionState.IN_GAME),
	ANSWER_PARTY_LOOT_MODIFICATION(0x76, AnswerPartyLootModification::new, ConnectionState.IN_GAME),
	ANSWER_COUPLE_ACTION(0x77, AnswerCoupleAction::new, ConnectionState.IN_GAME),
	BR_EVENT_RANKER_LIST(0x78, BrEventRankerList::new, ConnectionState.IN_GAME),
	REQUEST_ASK_MEMBER_SHIP(0x79, null, ConnectionState.IN_GAME),
	REQUEST_ADD_EXPAND_QUEST_ALARM(0x7A, RequestAddExpandQuestAlarm::new, ConnectionState.IN_GAME),
	REQUEST_VOTE_NEW(0x7B, RequestVoteNew::new, ConnectionState.IN_GAME),
	REQUEST_SHUTTLE_GET_ON(0x7C, RequestShuttleGetOn::new, ConnectionState.IN_GAME),
	REQUEST_SHUTTLE_GET_OFF(0x7D, RequestShuttleGetOff::new, ConnectionState.IN_GAME),
	MOVE_TO_LOCATION_IN_SHUTTLE(0x7E, MoveToLocationInShuttle::new, ConnectionState.IN_GAME),
	CANNOT_MORE_ANYMORE_IN_SHUTTLE(0x7F, CannotMoveAnymoreInShuttle::new, ConnectionState.IN_GAME),
	REQUEST_AGIT_ACTION(0x80, null, ConnectionState.IN_GAME), // TODO: Implement / HANDLE SWITCH
	REQUEST_EX_ADD_CONTACT_TO_CONTACT_LIST(0x81, RequestExAddContactToContactList::new, ConnectionState.IN_GAME),
	REQUEST_EX_DELETE_CONTACT_FROM_CONTACT_LIST(0x82, RequestExDeleteContactFromContactList::new, ConnectionState.IN_GAME),
	REQUEST_EX_SHOW_CONTACT_LIST(0x83, RequestExShowContactList::new, ConnectionState.IN_GAME),
	REQUEST_EX_FRIEND_LIST_EXTENDED(0x84, RequestExFriendListExtended::new, ConnectionState.IN_GAME),
	REQUEST_EX_OLYMPIAD_MATCH_LIST_REFRESH(0x85, RequestExOlympiadMatchListRefresh::new, ConnectionState.IN_GAME),
	REQUEST_BR_GAME_POINT(0x86, RequestBRGamePoint::new, ConnectionState.IN_GAME),
	REQUEST_BR_PRODUCT_LIST(0x87, RequestBRProductList::new, ConnectionState.IN_GAME),
	REQUEST_BR_PRODUCT_INFO(0x88, RequestBRProductInfo::new, ConnectionState.IN_GAME),
	REQUEST_BR_BUI_PRODUCT(0x89, RequestBRBuyProduct::new, ConnectionState.IN_GAME),
	REQUEST_BR_RECENT_PRODUCT_LIST(0x8A, RequestBRRecentProductList::new, ConnectionState.IN_GAME),
	REQUEST_BR_MINI_GAME_LOAD_SCORES(0x8B, null, ConnectionState.IN_GAME),
	REQUEST_BR_MINI_GAME_INSERT_SCORE(0x8C, null, ConnectionState.IN_GAME),
	REQUEST_EX_BR_LECTURE_MARK(0x8D, null, ConnectionState.IN_GAME),
	REQUEST_CRYSTALLIZE_ESTIMATE(0x8E, RequestCrystallizeEstimate::new, ConnectionState.IN_GAME),
	REQUEST_CRYSTALLIZE_ITEM_CANCEL(0x8F, RequestCrystallizeItemCancel::new, ConnectionState.IN_GAME),
	REQUEST_SCENE_EX_ESCAPE_SCENE(0x90, RequestExEscapeScene::new, ConnectionState.IN_GAME),
	REQUEST_FLY_MOVE(0x91, RequestFlyMove::new, ConnectionState.IN_GAME),
	REQUEST_SURRENDER_PLEDGE_WAR_EX(0x92, null, ConnectionState.IN_GAME),
	REQUEST_DYNAMIC_QUEST_ACTION(0x93, null, ConnectionState.IN_GAME), // TODO: Implement / HANDLE SWITCH
	REQUEST_FRIEND_DETAIL_INFO(0x94, RequestFriendDetailInfo::new, ConnectionState.IN_GAME),
	REQUEST_UPDATE_FRIEND_MEMO(0x95, null, ConnectionState.IN_GAME),
	REQUEST_UPDATE_BLOCK_MEMO(0x96, null, ConnectionState.IN_GAME),
	REQUEST_INZONE_PARTY_INFO_HISTORY(0x97, null, ConnectionState.IN_GAME),
	REQUEST_COMMISSION_REGISTRABLE_ITEM_LIST(0x98, RequestCommissionRegistrableItemList::new, ConnectionState.IN_GAME),
	REQUEST_COMMISSION_INFO(0x99, RequestCommissionInfo::new, ConnectionState.IN_GAME),
	REQUEST_COMMISSION_REGISTER(0x9A, RequestCommissionRegister::new, ConnectionState.IN_GAME),
	REQUEST_COMMISSION_CANCEL(0x9B, RequestCommissionCancel::new, ConnectionState.IN_GAME),
	REQUEST_COMMISSION_DELETE(0x9C, RequestCommissionDelete::new, ConnectionState.IN_GAME),
	REQUEST_COMMISSION_LIST(0x9D, RequestCommissionList::new, ConnectionState.IN_GAME),
	REQUEST_COMMISSION_BUY_INFO(0x9E, RequestCommissionBuyInfo::new, ConnectionState.IN_GAME),
	REQUEST_COMMISSION_BUY_ITEM(0x9F, RequestCommissionBuyItem::new, ConnectionState.IN_GAME),
	REQUEST_COMMISSION_REGISTERED_ITEM(0xA0, RequestCommissionRegisteredItem::new, ConnectionState.IN_GAME),
	REQUEST_CALL_TO_CHANGE_CLASS(0xA1, null, ConnectionState.IN_GAME),
	REQUEST_CHANGE_TO_AWAKENED_CLASS(0xA2, RequestChangeToAwakenedClass::new, ConnectionState.IN_GAME),
	REQUEST_WORLD_STATISTICS(0xA3, null, ConnectionState.IN_GAME),
	REQUEST_USER_STATISTICS(0xA4, null, ConnectionState.IN_GAME),
	REQUEST_24HZ_SESSION_ID(0xA5, null, ConnectionState.IN_GAME),
	REQUEST_GOODS_INVENTORY_INFO(0xAA, null, ConnectionState.IN_GAME),
	REQUEST_GOODS_INVENTORY_ITEM(0xAB, null, ConnectionState.IN_GAME),
	REQUEST_FIRST_PLAY_START(0xAC, null, ConnectionState.IN_GAME),
	REQUEST_FLY_MOVE_START(0xAD, RequestFlyMoveStart::new, ConnectionState.IN_GAME),
	REQUEST_HARDWARE_INFO(0xAE, null, ConnectionState.IN_GAME),
	SEND_CHANGE_ATTRIBUTE_TARGET_ITEM(0xB0, null, ConnectionState.IN_GAME),
	REQUEST_CHANGE_ATTRIBUTE_ITEM(0xB1, null, ConnectionState.IN_GAME),
	REQUEST_CHANGE_ATTRIBUTE_CANCEL(0xB2, null, ConnectionState.IN_GAME),
	REQUEST_BR_PRESENT_BUY_PRODUCT(0xB3, null, ConnectionState.IN_GAME),
	CONFIRM_MENTEE_ADD(0xB4, ConfirmMenteeAdd::new, ConnectionState.IN_GAME),
	REQUEST_MENTOR_CANCEL(0xB5, RequestMentorCancel::new, ConnectionState.IN_GAME),
	REQUEST_MENTOR_LIST(0xB6, RequestMentorList::new, ConnectionState.IN_GAME),
	REQUEST_MENTEE_ADD(0xB7, RequestMenteeAdd::new, ConnectionState.IN_GAME),
	REQUEST_MENTEE_WAITING_LIST(0xB8, RequestMenteeWaitingList::new, ConnectionState.IN_GAME),
	REQUEST_CLAN_ASK_JOIN_BY_NAME(0xB9, null, ConnectionState.IN_GAME),
	REQUEST_IN_ZONE_WAITING_TIME(0xBA, RequestInzoneWaitingTime::new, ConnectionState.IN_GAME),
	REQUEST_JOIN_CURIOUS_HOUSE(0xBB, RequestJoinCuriousHouse::new, ConnectionState.IN_GAME),
	REQUEST_CANCEL_CURIOUS_HOUSE(0xBC, RequestCancelCuriousHouse::new, ConnectionState.IN_GAME),
	REQUEST_LEAVE_CURIOUS_HOUSE(0xBD, null, ConnectionState.IN_GAME),
	REQUEST_OBSERVING_LIST_CURIOUS_HOUSE(0xBE, null, ConnectionState.IN_GAME),
	REQUEST_OBSERVING_CURIOUS_HOUSE(0xBF, null, ConnectionState.IN_GAME),
	REQUEST_LEAVE_OBSERVING_CURIOUS_HOUSE(0xC0, null, ConnectionState.IN_GAME),
	REQUEST_CURIOUS_HOUSE_HTML(0xC1, RequestCuriousHouseHtml::new, ConnectionState.IN_GAME),
	REQUEST_CURIOUS_HOUSE_RECORD(0xC2, null, ConnectionState.IN_GAME),
	EX_SYSSTRING(0xC3, null, ConnectionState.IN_GAME),
	REQUEST_EX_TRY_TO_ÜT_SHAPE_SHIFTING_TARGET_ITEM(0xC4, RequestExTryToPutShapeShiftingTargetItem::new, ConnectionState.IN_GAME),
	REQUEST_EX_TRY_TO_PUT_SHAPE_SHIFTING_ENCHANT_SUPPORT_ITEM(0xC5, RequestExTryToPutShapeShiftingEnchantSupportItem::new, ConnectionState.IN_GAME),
	REQUEST_EX_CANCEL_SHAPE_SHIFTING_ITEM(0xC6, RequestExCancelShape_Shifting_Item::new, ConnectionState.IN_GAME),
	REQUEST_SHAPE_SHIFTING_ITEM(0xC7, RequestShapeShiftingItem::new, ConnectionState.IN_GAME),
	NC_GUARD_SEND_DATA_TO_SERVER(0xC8, null, ConnectionState.IN_GAME),
	REQUEST_EVENT_KALIE_TOKEN(0xC9, null, ConnectionState.IN_GAME),
	REQUEST_SHOW_BEAUTY_LIST(0xCA, RequestShowBeautyList::new, ConnectionState.IN_GAME),
	REQUEST_REGIST_BEAUTY(0xCB, RequestRegistBeauty::new, ConnectionState.IN_GAME),
	REQUEST_SHOW_RESET_SHOP_LIST(0xCD, RequestShowResetShopList::new, ConnectionState.IN_GAME),
	NET_PING(0xCE, null, ConnectionState.IN_GAME),
	REQUEST_BR_ADD_BASKET_PRODUCT_INFO(0xCF, null, ConnectionState.IN_GAME),
	REQUEST_BR_DELETE_BASKET_PRODUCT_INFO(0xD0, null, ConnectionState.IN_GAME),
	REQUEST_EX_EVENT_CAMPAIGN_INFO(0xD2, null, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_RECRUIT_INFO(0xD3, RequestPledgeRecruitInfo::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_RECRUIT_BOARD_SEARCH(0xD4, RequestPledgeRecruitBoardSearch::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_RECRUIT_BOARD_ACCESS(0xD5, RequestPledgeRecruitBoardAccess::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_RECRUIT_BOARD_DETAIL(0xD6, RequestPledgeRecruitBoardDetail::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_WAITING_APPLY(0xD7, RequestPledgeWaitingApply::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_WAITING_APPLIED(0xD8, RequestPledgeWaitingApplied::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_WAITING_LIST(0xD9, RequestPledgeWaitingList::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_WAITING_USER(0xDA, RequestPledgeWaitingUser::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_WAITING_USER_ACCEPT(0xDB, RequestPledgeWaitingUserAccept::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_DRAFT_LIST_SEARCH(0xDC, RequestPledgeDraftListSearch::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_DRAFT_LIST_APPLY(0xDD, RequestPledgeDraftListApply::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_RECRUIT_APPLY_INFO(0xDE, RequestPledgeRecruitApplyInfo::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_JOIN_SYS(0xDF, null, ConnectionState.IN_GAME),
	RESPONSE_PETITION_ALARM(0xE0, null, ConnectionState.IN_GAME),
	NOTIFY_EXIT_BEAUTY_SHOP(0xE1, NotifyExitBeautyShop::new, ConnectionState.IN_GAME),
	REQUEST_REGISTER_XMAS_WISH_CARD(0xE2, null, ConnectionState.IN_GAME),
	REQUEST_EX_ADD_ENCHANT_SCROLL_ITEM(0xE3, RequestExAddEnchantScrollItem::new, ConnectionState.IN_GAME),
	REQUEST_EX_REMOVE_ENCHANT_SUPPORT_ITEM(0xE4, RequestExRemoveEnchantSupportItem::new, ConnectionState.IN_GAME),
	REQUEST_CARD_REWARD(0xE5, null, ConnectionState.IN_GAME),
	REQUEST_DIVIDE_ADENA_START(0xE6, RequestDivideAdenaStart::new, ConnectionState.IN_GAME),
	REQUEST_DIVIDE_ADENA_CANCEL(0xE7, RequestDivideAdenaCancel::new, ConnectionState.IN_GAME),
	REQUEST_DIVIDE_ADENA(0xE8, RequestDivideAdena::new, ConnectionState.IN_GAME),
	REQUEST_ACQUIRE_ABILITY_LIST(0xE9, RequestAcquireAbilityList::new, ConnectionState.IN_GAME),
	REQUEST_ABILITY_LIST(0xEA, RequestAbilityList::new, ConnectionState.IN_GAME),
	REQUEST_RESET_ABILITY_POINT(0xEB, RequestResetAbilityPoint::new, ConnectionState.IN_GAME),
	REQUEST_CHANGE_ABILITY_POINT(0xEC, RequestChangeAbilityPoint::new, ConnectionState.IN_GAME),
	REQUEST_STOP_MOVE(0xED, null, ConnectionState.IN_GAME),
	REQUEST_ABILITY_WND_OPEN(0xEE, RequestAbilityWndOpen::new, ConnectionState.IN_GAME),
	REQUEST_ABILITY_WND_CLOSE(0xEF, RequestAbilityWndClose::new, ConnectionState.IN_GAME),
	EX_PC_CAFE_REQUEST_OPEN_WINDOW_WITHOUT_NPC(0xF0, null, ConnectionState.IN_GAME),
	REQUEST_LUCKY_GAME_PLAY(0xF2, null, ConnectionState.IN_GAME),
	NOTIFY_TRAINING_ROOM_END(0xF3, null, ConnectionState.IN_GAME),
	REQUEST_NEW_ENCHANT_PUSH_ONE(0xF4, RequestNewEnchantPushOne::new, ConnectionState.IN_GAME),
	REQUEST_NEW_ENCHANT_REMOVE_ONE(0xF5, RequestNewEnchantRemoveOne::new, ConnectionState.IN_GAME),
	REQUEST_NEW_ENCHANT_PUSH_TWO(0xF6, RequestNewEnchantPushTwo::new, ConnectionState.IN_GAME),
	REQUEST_NEW_ENCHANT_REMOVE_TWO(0xF7, RequestNewEnchantRemoveTwo::new, ConnectionState.IN_GAME),
	REQUEST_NEW_ENCHANT_CLOSE(0xF8, RequestNewEnchantClose::new, ConnectionState.IN_GAME),
	REQUEST_NEW_ENCHANT_TRY(0xF9, RequestNewEnchantTry::new, ConnectionState.IN_GAME),
	EX_SEND_SELECTED_QUEST_ZONE_ID(0xFE, ExSendSelectedQuestZoneID::new, ConnectionState.IN_GAME),
	REQUEST_ALCHEMY_SKILL_LIST(0xFF, RequestAlchemySkillList::new, ConnectionState.IN_GAME),
	REQUEST_ALCHEMY_TRY_MIX_CUBE(0x100, RequestAlchemyTryMixCube::new, ConnectionState.IN_GAME),
	REQUEST_ALCHEMY_CONVERSION(0x101, RequestAlchemyConversion::new, ConnectionState.IN_GAME),
	SEND_EXECUTED_UI_EVENTS_COUNT(0x102, null, ConnectionState.IN_GAME),
	EX_SEND_CLIENT_INI(0x103, null, ConnectionState.IN_GAME),
	REQUEST_EX_AUTO_FISH(0x104, ExRequestAutoFish::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_BONUS_OPEN(0x112, RequestPledgeBonusOpen::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_BONUS_REWARD_LIST(0x113, RequestPledgeBonusRewardList::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_BONUS_REWARD(0x114, RequestPledgeBonusReward::new, ConnectionState.IN_GAME);
	
	public static final ExIncomingPackets[] PACKET_ARRAY;
	
	static
	{
		final short maxPacketId = (short) Arrays.stream(values()).mapToInt(IIncomingPackets::getPacketId).max().orElse(0);
		PACKET_ARRAY = new ExIncomingPackets[maxPacketId + 1];
		for (ExIncomingPackets incomingPacket : values())
		{
			PACKET_ARRAY[incomingPacket.getPacketId()] = incomingPacket;
		}
	}
	
	private int _packetId;
	private Supplier<IIncomingPacket<L2GameClient>> _incomingPacketFactory;
	private Set<IConnectionState> _connectionStates;
	
	ExIncomingPackets(int packetId, Supplier<IIncomingPacket<L2GameClient>> incomingPacketFactory, IConnectionState... connectionStates)
	{
		// packetId is an unsigned short
		if (packetId > 0xFFFF)
		{
			throw new IllegalArgumentException("packetId must not be bigger than 0xFFFF");
		}
		_packetId = packetId;
		_incomingPacketFactory = incomingPacketFactory != null ? incomingPacketFactory : () -> null;
		_connectionStates = new HashSet<>(Arrays.asList(connectionStates));
	}
	
	@Override
	public int getPacketId()
	{
		return _packetId;
	}
	
	@Override
	public IIncomingPacket<L2GameClient> newIncomingPacket()
	{
		return _incomingPacketFactory.get();
	}
	
	@Override
	public Set<IConnectionState> getConnectionStates()
	{
		return _connectionStates;
	}
}
