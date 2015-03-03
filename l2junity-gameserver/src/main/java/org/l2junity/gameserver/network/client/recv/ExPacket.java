/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver.network.client.recv;

import org.l2junity.gameserver.network.client.L2GameClient;
import org.l2junity.gameserver.network.client.recv.adenadistribution.RequestDivideAdena;
import org.l2junity.gameserver.network.client.recv.adenadistribution.RequestDivideAdenaCancel;
import org.l2junity.gameserver.network.client.recv.adenadistribution.RequestDivideAdenaStart;
import org.l2junity.gameserver.network.client.recv.appearance.RequestExCancelShape_Shifting_Item;
import org.l2junity.gameserver.network.client.recv.appearance.RequestExTryToPutShapeShiftingEnchantSupportItem;
import org.l2junity.gameserver.network.client.recv.appearance.RequestExTryToPutShapeShiftingTargetItem;
import org.l2junity.gameserver.network.client.recv.appearance.RequestShapeShiftingItem;
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
import org.l2junity.network.IIncomingPacket;
import org.l2junity.network.PacketReader;

/**
 * @author Nos
 */
public class ExPacket implements IIncomingPacket<L2GameClient>
{
	private IIncomingPacket<L2GameClient> _exPacket;
	
	@Override
	public boolean read(PacketReader packet)
	{
		int exPacketId = packet.readH() & 0xFFFF;
		switch (exPacketId)
		{
			case 0x00:
				break;
			case 0x01:
				_exPacket = new RequestManorList();
				break;
			case 0x02:
				_exPacket = new RequestProcureCropList();
				break;
			case 0x03:
				_exPacket = new RequestSetSeed();
				break;
			case 0x04:
				_exPacket = new RequestSetCrop();
				break;
			case 0x05:
				_exPacket = new RequestWriteHeroWords();
				break;
			case 0x06:
				_exPacket = new RequestExAskJoinMPCC();
				break;
			case 0x07:
				_exPacket = new RequestExAcceptJoinMPCC();
				break;
			case 0x08:
				_exPacket = new RequestExOustFromMPCC();
				break;
			case 0x09:
				_exPacket = new RequestOustFromPartyRoom();
				break;
			case 0x0a:
				_exPacket = new RequestDismissPartyRoom();
				break;
			case 0x0b:
				_exPacket = new RequestWithdrawPartyRoom();
				break;
			case 0x0c:
				_exPacket = new RequestChangePartyLeader(); // RequestHandOverPartyMaster ?
				break;
			case 0x0d:
				_exPacket = new RequestAutoSoulShot();
				break;
			case 0x0e:
				_exPacket = new RequestExEnchantSkillInfo();
				break;
			case 0x0f:
				_exPacket = new RequestExEnchantSkill();
				break;
			case 0x10:
				_exPacket = new RequestExPledgeCrestLarge();
				break;
			case 0x11:
				int id9 = packet.readD();
				switch (id9)
				{
				// @TODO: : RequestExSetPledgeCrestLarge is now chunked, each case is a different chunk
					case 0x00:
					{
						_exPacket = new RequestExSetPledgeCrestLarge();// 0
						break;
					}
					case 0x01:
					{
						_exPacket = new RequestExSetPledgeCrestLarge();// 1
						break;
					}
					case 0x02:
					{
						_exPacket = new RequestExSetPledgeCrestLarge();// 2
						break;
					}
					case 0x03:
					{
						_exPacket = new RequestExSetPledgeCrestLarge();// 3
						break;
					}
					case 0x04:
					{
						_exPacket = new RequestExSetPledgeCrestLarge();// 4
						break;
					}
				}
				break;
			case 0x12:
				_exPacket = new RequestPledgeSetAcademyMaster();
				break;
			case 0x13:
				_exPacket = new RequestPledgePowerGradeList();
				break;
			case 0x14:
				_exPacket = new RequestPledgeMemberPowerInfo();
				break;
			case 0x15:
				_exPacket = new RequestPledgeSetMemberPowerGrade();
				break;
			case 0x16:
				_exPacket = new RequestPledgeMemberInfo();
				break;
			case 0x17:
				_exPacket = new RequestPledgeWarList();
				break;
			case 0x18:
				_exPacket = new RequestExFishRanking();
				break;
			case 0x19:
				_exPacket = new RequestPCCafeCouponUse();
				break;
			case 0x1b:
				_exPacket = new RequestDuelStart();
				break;
			case 0x1c:
				_exPacket = new RequestDuelAnswerStart();
				break;
			case 0x1d:
				// RequestExSetTutorial
				break;
			case 0x1e:
				_exPacket = new RequestExRqItemLink();
				break;
			case 0x1f:
				// CanNotMoveAnymoreAirShip
				break;
			case 0x20:
				_exPacket = new MoveToLocationInAirShip();
				break;
			case 0x21:
				_exPacket = new RequestKeyMapping();
				break;
			case 0x22:
				_exPacket = new RequestSaveKeyMapping();
				break;
			case 0x23:
				_exPacket = new RequestExRemoveItemAttribute();
				break;
			case 0x24:
				_exPacket = new RequestSaveInventoryOrder();
				break;
			case 0x25:
				_exPacket = new RequestExitPartyMatchingWaitingRoom();
				break;
			case 0x26:
				_exPacket = new RequestConfirmTargetItem();
				break;
			case 0x27:
				_exPacket = new RequestConfirmRefinerItem();
				break;
			case 0x28:
				_exPacket = new RequestConfirmGemStone();
				break;
			case 0x29:
				_exPacket = new RequestOlympiadObserverEnd();
				break;
			case 0x2a:
				_exPacket = new RequestCursedWeaponList();
				break;
			case 0x2b:
				_exPacket = new RequestCursedWeaponLocation();
				break;
			case 0x2c:
				_exPacket = new RequestPledgeReorganizeMember();
				break;
			case 0x2d:
				_exPacket = new RequestExMPCCShowPartyMembersInfo();
				break;
			case 0x2e:
				_exPacket = new RequestOlympiadMatchList();
				break;
			case 0x2f:
				_exPacket = new RequestAskJoinPartyRoom();
				break;
			case 0x30:
				_exPacket = new AnswerJoinPartyRoom();
				break;
			case 0x31:
				_exPacket = new RequestListPartyMatchingWaitingRoom();
				break;
			case 0x32:
				_exPacket = new RequestExEnchantItemAttribute();
				break;
			case 0x35:
				_exPacket = new MoveToLocationAirShip();
				break;
			case 0x36:
				_exPacket = new RequestBidItemAuction();
				break;
			case 0x37:
				_exPacket = new RequestInfoItemAuction();
				break;
			case 0x38:
				_exPacket = new RequestExChangeName();
				break;
			case 0x39:
				_exPacket = new RequestAllCastleInfo();
				break;
			case 0x3a:
				_exPacket = new RequestAllFortressInfo();
				break;
			case 0x3b:
				_exPacket = new RequestAllAgitInfo();
				break;
			case 0x3c:
				_exPacket = new RequestFortressSiegeInfo();
				break;
			case 0x3d:
				_exPacket = new RequestGetBossRecord();
				break;
			case 0x3e:
				_exPacket = new RequestRefine();
				break;
			case 0x3f:
				_exPacket = new RequestConfirmCancelItem();
				break;
			case 0x40:
				_exPacket = new RequestRefineCancel();
				break;
			case 0x41:
				_exPacket = new RequestExMagicSkillUseGround();
				break;
			case 0x42:
				_exPacket = new RequestDuelSurrender();
				break;
			case 0x43:
				_exPacket = new RequestExEnchantSkillInfoDetail();
				break;
			case 0x44:
				_exPacket = new RequestExMagicSkillUseGround();
				break;
			case 0x45:
				_exPacket = new RequestFortressMapInfo();
				break;
			case 0x46:
				_exPacket = new RequestPVPMatchRecord();
				break;
			case 0x47:
				_exPacket = new SetPrivateStoreWholeMsg();
				break;
			case 0x48:
				_exPacket = new RequestDispel();
				break;
			case 0x49:
				_exPacket = new RequestExTryToPutEnchantTargetItem();
				break;
			case 0x4a:
				_exPacket = new RequestExTryToPutEnchantSupportItem();
				break;
			case 0x4b:
				_exPacket = new RequestExCancelEnchantItem();
				break;
			case 0x4c:
				_exPacket = new RequestChangeNicknameColor();
				break;
			case 0x4d:
				_exPacket = new RequestResetNickname();
				break;
			case 0x4e:
				int id4 = packet.readD();
				switch (id4)
				{
					case 0x00:
						_exPacket = new RequestBookMarkSlotInfo();
						break;
					case 0x01:
						_exPacket = new RequestSaveBookMarkSlot();
						break;
					case 0x02:
						_exPacket = new RequestModifyBookMarkSlot();
						break;
					case 0x03:
						_exPacket = new RequestDeleteBookMarkSlot();
						break;
					case 0x04:
						_exPacket = new RequestTeleportBookMark();
						break;
					case 0x05:
						_exPacket = new RequestChangeBookMarkSlot();
						break;
				}
				break;
			case 0x4f:
				_exPacket = new RequestWithDrawPremiumItem();
				break;
			case 0x50:
				// @TODO: _exPacket = new RequestExJump();
				break;
			case 0x51:
				// @TODO: _exPacket = new RequestExStartShowCrataeCubeRank();
				break;
			case 0x52:
				// @TODO: _exPacket = new RequestExStopShowCrataeCubeRank();
				break;
			case 0x53:
				// @TODO: _exPacket = new NotifyStartMiniGame();
				break;
			case 0x54:
				// @TODO: _exPacket = new RequestExJoinDominionWar();
				break;
			case 0x55:
				// @TODO: _exPacket = new RequestExDominionInfo();
				break;
			case 0x56:
				// @TODO: _exPacket = new RequestExCleftEnter();
				break;
			case 0x57:
				_exPacket = new RequestExCubeGameChangeTeam();
				break;
			case 0x58:
				_exPacket = new EndScenePlayer();
				break;
			case 0x59:
				_exPacket = new RequestExCubeGameReadyAnswer();
				break;
			case 0x5A:
				_exPacket = new RequestExListMpccWaiting();
				break;
			case 0x5B:
				_exPacket = new RequestExManageMpccRoom();
				break;
			case 0x5C:
				_exPacket = new RequestExJoinMpccRoom();
				break;
			case 0x5D:
				_exPacket = new RequestExOustFromMpccRoom();
				break;
			case 0x5E:
				_exPacket = new RequestExDismissMpccRoom();
				break;
			case 0x5F:
				_exPacket = new RequestExWithdrawMpccRoom();
				break;
			case 0x60:
				_exPacket = new RequestSeedPhase();
				break;
			case 0x61:
				_exPacket = new RequestExMpccPartymasterList();
				break;
			case 0x62:
				_exPacket = new RequestPostItemList();
				break;
			case 0x63:
				_exPacket = new RequestSendPost();
				break;
			case 0x64:
				_exPacket = new RequestReceivedPostList();
				break;
			case 0x65:
				_exPacket = new RequestDeleteReceivedPost();
				break;
			case 0x66:
				_exPacket = new RequestReceivedPost();
				break;
			case 0x67:
				_exPacket = new RequestPostAttachment();
				break;
			case 0x68:
				_exPacket = new RequestRejectPostAttachment();
				break;
			case 0x69:
				_exPacket = new RequestSentPostList();
				break;
			case 0x6a:
				_exPacket = new RequestDeleteSentPost();
				break;
			case 0x6b:
				_exPacket = new RequestSentPost();
				break;
			case 0x6c:
				_exPacket = new RequestCancelPostAttachment();
				break;
			case 0x6d:
				// @TODO: RequestShowNewUserPetition
				break;
			case 0x6e:
				// @TODO: RequestShowStepTwo
				break;
			case 0x6f:
				// @TODO: RequestShowStepThree
				break;
			case 0x70:
				// _exPacket = new ExConnectToRaidServer(); (chddd)
				break;
			case 0x71:
				// _exPacket = new ExReturnFromRaidServer(); (chd)
				break;
			case 0x72:
				_exPacket = new RequestRefundItem();
				break;
			case 0x73:
				_exPacket = new RequestBuySellUIClose();
				break;
			case 0x75:
				_exPacket = new RequestPartyLootModification();
				break;
			case 0x76:
				_exPacket = new AnswerPartyLootModification();
				break;
			case 0x77:
				_exPacket = new AnswerCoupleAction();
				break;
			case 0x78:
				_exPacket = new BrEventRankerList();
				break;
			case 0x79:
				// _exPacket = new RequestAskMemberShip();
				break;
			case 0x7a:
				_exPacket = new RequestAddExpandQuestAlarm();
				break;
			case 0x7b:
				_exPacket = new RequestVoteNew();
				break;
			case 0x7c:
				_exPacket = new RequestShuttleGetOn();
				break;
			case 0x7d:
				_exPacket = new RequestShuttleGetOff();
				break;
			case 0x7e:
				_exPacket = new MoveToLocationInShuttle();
				break;
			case 0x7F:
				_exPacket = new CannotMoveAnymoreInShuttle();
				break;
			case 0x80:
				break;
			case 0x81:
				_exPacket = new RequestExAddContactToContactList(); // RequestExAddPostFriendForPostBox
				break;
			case 0x82:
				_exPacket = new RequestExDeleteContactFromContactList(); // RequestExDeletePostFriendForPostBox
				break;
			case 0x83:
				_exPacket = new RequestExShowContactList(); // RequestExShowPostFriendListForPostBox
				break;
			case 0x84:
				_exPacket = new RequestExFriendListExtended(); // RequestExFriendListForPostBox
				break;
			case 0x85:
				_exPacket = new RequestExOlympiadMatchListRefresh(); // RequestOlympiadMatchList
				break;
			case 0x86:
				_exPacket = new RequestBRGamePoint();
				break;
			case 0x87:
				_exPacket = new RequestBRProductList();
				break;
			case 0x88:
				_exPacket = new RequestBRProductInfo();
				break;
			case 0x89:
				_exPacket = new RequestBRBuyProduct();
				break;
			case 0x8A:
				_exPacket = new RequestBRRecentProductList();
				break;
			case 0x8B:
				// @ _exPacket = new RequestBR_MiniGameLoadScores();
				break;
			case 0x8C:
				// @ _exPacket = new RequestBR_MiniGameInsertScore();
				break;
			case 0x8D:
				// @ _exPacket = new RequestExBR_LectureMark();
				break;
			case 0x8E:
				_exPacket = new RequestCrystallizeEstimate();
				break;
			case 0x8F:
				_exPacket = new RequestCrystallizeItemCancel();
				break;
			case 0x90:
				// @ _exPacket = new RequestExEscapeScene();
				break;
			case 0x91:
				_exPacket = new RequestFlyMove();
				break;
			case 0x92:
				// _exPacket = new RequestSurrenderPledgeWarEX(); (chS)
				break;
			case 0x93:
			{
				int id6 = packet.readD();
				switch (id6)
				{
					case 0x02:
						// _exPacket = new RequestDynamicQuestProgressInfo()
						break;
					case 0x03:
						// _exPacket = new RequestDynamicQuestScoreBoard();
						break;
					case 0x04:
						// _exPacket = new RequestDynamicQuestHTML();
						break;
				}
				
				break;
			}
			case 0x94:
				_exPacket = new RequestFriendDetailInfo();
				break;
			case 0x95:
				// _exPacket = new RequestUpdateFriendMemo();
				break;
			case 0x96:
				// _exPacket = new RequestUpdateBlockMemo();
				break;
			case 0x97:
				// _exPacket = new RequestInzonePartyInfoHistory();
				break;
			case 0x98:
				_exPacket = new RequestCommissionRegistrableItemList();
				break;
			case 0x99:
				_exPacket = new RequestCommissionInfo();
				break;
			case 0x9A:
				_exPacket = new RequestCommissionRegister();
				break;
			case 0x9B:
				_exPacket = new RequestCommissionCancel();
				break;
			case 0x9C:
				_exPacket = new RequestCommissionDelete();
				break;
			case 0x9D:
				_exPacket = new RequestCommissionList();
				break;
			case 0x9E:
				_exPacket = new RequestCommissionBuyInfo();
				break;
			case 0x9F:
				_exPacket = new RequestCommissionBuyItem();
				break;
			case 0xA0:
				_exPacket = new RequestCommissionRegisteredItem();
				break;
			case 0xA1:
				// _exPacket = new RequestCallToChangeClass();
				break;
			case 0xA2:
				_exPacket = new RequestChangeToAwakenedClass();
				break;
			case 0xA3:
				// _exPacket = new RequestWorldStatistics();
				break;
			case 0xA4:
				// _exPacket = new RequestUserStatistics();
				break;
			case 0xA5:
				// _exPacket = new Request24HzSessionID();
				break;
			case 0xAA:
				// _exPacket = new RequestGoodsInventoryInfo();
				break;
			case 0xAB:
				// _exPacket = new RequestUseGoodsInventoryItem();
				break;
			case 0xAC:
				// _exPacket = new RequestFirstPlayStart();
				break;
			case 0xAD:
				_exPacket = new RequestFlyMoveStart();
				break;
			case 0xAE:
			case 0xAF:
				// _exPacket = new RequestHardWareInfo(); (SddddddSddddddddddSS)
				break;
			case 0xB0:
				// _exPacket = new SendChangeAttributeTargetItem();
				break;
			case 0xB1:
				// _exPacket = new RequestChangeAttributeItem();
				break;
			case 0xB2:
				// _exPacket = new RequestChangeAttributeCancel();
				break;
			case 0xB3:
				// _exPacket = new RequestBR_PresentBuyProduct();
				break;
			case 0xB4:
				_exPacket = new ConfirmMenteeAdd();
				break;
			case 0xB5:
				_exPacket = new RequestMentorCancel();
				break;
			case 0xB6:
				_exPacket = new RequestMentorList();
				break;
			case 0xB7:
				_exPacket = new RequestMenteeAdd();
				break;
			case 0xB8:
				_exPacket = new RequestMenteeWaitingList();
				break;
			case 0xB9:
				// _exPacket = new RequestClanAskJoinByName();
				break;
			case 0xBA:
				_exPacket = new RequestInzoneWaitingTime();
				break;
			case 0xBB:
				// _exPacket = new RequestJoinCuriousHouse();// (ch)
				break;
			case 0xBC:
				// _exPacket = new RequestCancelCuriousHouse();// (ch)
				break;
			case 0xBD:
				// _exPacket = new RequestLeaveCuriousHouse();// (ch)
				break;
			case 0xBE:
				// _exPacket = new RequestObservingListCuriousHouse();// (ch)
				break;
			case 0xBF:
				// _exPacket = new RequestObservingCuriousHouse();// (chd)
				break;
			case 0xC0:
				// _exPacket = new RequestLeaveObservingCuriousHouse();// (ch)
				break;
			case 0xC1:
				// _exPacket = new RequestCuriousHouseHtml();// (ch)
				break;
			case 0xC2:
				// _exPacket = new RequestCuriousHouseRecord();// (ch)
				break;
			case 0xC3:
				// _exPacket = new ExSysstring(); (chdS)
				break;
			case 0xC4:
				_exPacket = new RequestExTryToPutShapeShiftingTargetItem();
				break;
			case 0xC5:
				_exPacket = new RequestExTryToPutShapeShiftingEnchantSupportItem();
				break;
			case 0xC6:
				_exPacket = new RequestExCancelShape_Shifting_Item();
				break;
			case 0xC7:
				_exPacket = new RequestShapeShiftingItem();
				break;
			case 0xC8:
				// _exPacket = new NCGuardSendDataToServer(); // (chdb)
				break;
			case 0xC9:
				// _exPacket = new RequestEventKalieToken(); // (d)
				break;
			case 0xCA:
				_exPacket = new RequestShowBeautyList();
				break;
			case 0xCB:
				_exPacket = new RequestRegistBeauty();
				break;
			case 0xCC:
				break;
			case 0xCD:
				_exPacket = new RequestShowResetShopList();
				break;
			case 0xCE:
				// NetPing
				break;
			case 0xCF:
				// RequestBR_AddBasketProductInfo
				break;
			case 0xD0:
				// RequestBR_DeleteBasketProductInfo
				break;
			case 0xD1:
				break;
			case 0xD2:
				// RequestExEvent_Campaign_Info
				break;
			case 0xD3:
				_exPacket = new RequestPledgeRecruitInfo();
				break;
			case 0xD4:
				_exPacket = new RequestPledgeRecruitBoardSearch();
				break;
			case 0xD5:
				_exPacket = new RequestPledgeRecruitBoardAccess();
				break;
			case 0xD6:
				_exPacket = new RequestPledgeRecruitBoardDetail();
				break;
			case 0xD7:
				_exPacket = new RequestPledgeWaitingApply();
				break;
			case 0xD8:
				_exPacket = new RequestPledgeWaitingApplied();
				break;
			case 0xD9:
				_exPacket = new RequestPledgeWaitingList();
				break;
			case 0xDA:
				_exPacket = new RequestPledgeWaitingUser();
				break;
			case 0xDB:
				_exPacket = new RequestPledgeWaitingUserAccept();
				break;
			case 0xDC:
				_exPacket = new RequestPledgeDraftListSearch();
				break;
			case 0xDD:
				_exPacket = new RequestPledgeDraftListApply();
				break;
			case 0xDE:
				_exPacket = new RequestPledgeRecruitApplyInfo();
				break;
			case 0xDF:
				// _exPacket = new RequestPledgeJoinSys();
				break;
			case 0xE0:
				// ResponsePetitionAlarm
				break;
			case 0xE1:
				_exPacket = new NotifyExitBeautyShop();
				break;
			case 0xE2:
				// RequestRegisterXMasWishCard
				break;
			case 0xE3:
				_exPacket = new RequestExAddEnchantScrollItem();
				break;
			case 0xE4:
				_exPacket = new RequestExRemoveEnchantSupportItem();
				break;
			case 0xE5:
				// _exPacket = new RequestCardReward();
				break;
			case 0xE6:
				_exPacket = new RequestDivideAdenaStart();
				break;
			case 0xE7:
				_exPacket = new RequestDivideAdenaCancel();
				break;
			case 0xE8:
				_exPacket = new RequestDivideAdena();
				break;
			case 0xE9:
				_exPacket = new RequestAcquireAbilityList();
				break;
			case 0xEA:
				_exPacket = new RequestAbilityList();
				break;
			case 0xEB:
				_exPacket = new RequestResetAbilityPoint();
				break;
			case 0xEC:
				_exPacket = new RequestChangeAbilityPoint();
				break;
			case 0xED:
				// _exPacket = new RequestStopMove();
				break;
			case 0xEE:
				_exPacket = new RequestAbilityWndOpen();
				break;
			case 0xEF:
				_exPacket = new RequestAbilityWndClose();
				break;
			case 0xF0:
				// _exPacket = ExPCCafeRequestOpenWindowWithoutNPC();
				break;
			case 0xF2:
				// _exPacket = new RequestLuckyGamePlay();
				break;
			case 0xF3:
				// _exPacket = new NotifyTrainingRoomEnd();
				break;
			case 0xF4:
				_exPacket = new RequestNewEnchantPushOne();
				break;
			case 0xF5:
				_exPacket = new RequestNewEnchantRemoveOne();
				break;
			case 0xF6:
				_exPacket = new RequestNewEnchantPushTwo();
				break;
			case 0xF7:
				_exPacket = new RequestNewEnchantRemoveTwo();
				break;
			case 0xF8:
				_exPacket = new RequestNewEnchantClose();
				break;
			case 0xF9:
				_exPacket = new RequestNewEnchantTry();
				break;
			case 0xFE:
				// _exPacket = new ExSendSelectedQuestZoneID();
				break;
			case 0xFF:
				_exPacket = new RequestAlchemySkillList();
				break;
			case 0x100:
				// _exPacket = new RequestAlchemyTryMixCube();
				break;
			case 0x101:
				// _exPacket = new RequestAlchemyConversion();
				break;
			case 0x102:
				// _exPacket = new SendExecutedUIEventsCount();
				break;
			case 0x103:
				// _exPacket = new ExSendClientINI();
				break;
			case 0x104:
				// _exPacket = new RequestExAutoFish();
				break;
			case 0x33:
				_exPacket = new RequestGotoLobby();
				break;
			case 0xA6:
				_exPacket = new RequestEx2ndPasswordCheck();
				break;
			case 0xA7:
				_exPacket = new RequestEx2ndPasswordVerify();
				break;
			case 0xA8:
				_exPacket = new RequestEx2ndPasswordReq();
				break;
			case 0xA9:
				_exPacket = new RequestCharacterNameCreatable();
				break;
		}
		return (_exPacket != null) && _exPacket.read(packet);
	}
	
	@Override
	public void run(L2GameClient client) throws Exception
	{
		_exPacket.run(client);
	}
}
