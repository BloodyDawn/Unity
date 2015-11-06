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
package handlers;

import org.l2junity.gameserver.handler.EffectHandler;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import handlers.effecthandlers.*;

/**
 * Effect Master handler.
 * @author BiggBoss, Zoey76
 */
public final class EffectMasterHandler
{
	private static final Logger LOGGER = LoggerFactory.getLogger(EffectMasterHandler.class);
	
	private static final Class<?>[] EFFECTS =
	{
		AbnormalTimeChange.class,
		AddHate.class,
		AddTeleportBookmarkSlot.class,
		AttackTrait.class,
		Backstab.class,
		Betray.class,
		Blink.class,
		BlinkSwap.class,
		BlockAction.class,
		BlockActions.class,
		BlockChat.class,
		BlockControl.class,
		BlockEscape.class,
		BlockParty.class,
		BlockBuffSlot.class,
		BlockResurrection.class,
		BlockTarget.class,
		Bluff.class,
		Buff.class,
		CallParty.class,
		CallPc.class,
		CallSkill.class,
		CallSkillOnActionTime.class,
		CallTargetParty.class,
		ChameleonRest.class,
		ChangeFace.class,
		ChangeFishingMastery.class,
		ChangeHairColor.class,
		ChangeHairStyle.class,
		ClassChange.class,
		Confuse.class,
		ConsumeBody.class,
		ConvertItem.class,
		CpDamPercent.class,
		CpHeal.class,
		CpHealOverTime.class,
		CpHealPercent.class,
		CreateItemRandom.class,
		CrystalGradeModify.class,
		CubicMastery.class,
		DamageShield.class,
		DamOverTime.class,
		DamOverTimePercent.class,
		DeathLink.class,
		Debuff.class,
		DebuffBlock.class,
		DefenceTrait.class,
		DeleteHate.class,
		DeleteHateOfMe.class,
		DeleteTopAgro.class,
		DetectHiddenObjects.class,
		Detection.class,
		DisableTargeting.class,
		Disarm.class,
		Disarmor.class,
		DispelAll.class,
		DispelByCategory.class,
		DispelBySlot.class,
		DispelBySlotMyself.class,
		DispelBySlotProbability.class,
		EnableCloak.class,
		EnemyCharge.class,
		EnergyAttack.class,
		EnlargeAbnormalSlot.class,
		Escape.class,
		Faceoff.class,
		FakeDeath.class,
		FatalBlow.class,
		Fear.class,
		Feed.class,
		Flag.class,
		FlyMove.class,
		FocusMomentum.class,
		FocusMaxMomentum.class,
		FocusSouls.class,
		GetAgro.class,
		GetMomentum.class,
		GiveRecommendation.class,
		GiveSp.class,
		Grow.class,
		HairAccessorySet.class,
		Harvesting.class,
		HeadquarterCreate.class,
		Heal.class,
		HealOverTime.class,
		HealPercent.class,
		Hide.class,
		HpByLevel.class,
		HpCpHeal.class,
		HpDrain.class,
		ImmobileBuff.class,
		ImmobilePetBuff.class,
		Invincible.class,
		KarmaCount.class,
		KnockBack.class,
		Lethal.class,
		Lucky.class,
		MagicalAbnormalDispelAttack.class,
		MagicalAttack.class,
		MagicalAttackByAbnormal.class,
		MagicalAttackMp.class,
		MagicalAttackRange.class,
		MagicalDamOverTime.class,
		MagicalSoulAttack.class,
		ManaDamOverTime.class,
		ManaHeal.class,
		ManaHealByLevel.class,
		ManaHealOverTime.class,
		ManaHealPercent.class,
		MaxHp.class,
		ModifyVital.class,
		MpConsumePerLevel.class,
		Mute.class,
		NoblesseBless.class,
		OpenChest.class,
		Unsummon.class,
		OpenCommonRecipeBook.class,
		OpenDoor.class,
		OpenDwarfRecipeBook.class,
		Passive.class,
		Plunder.class,
		PhysicalAttack.class,
		PhysicalAttackHpLink.class,
		PhysicalAttackMute.class,
		PhysicalAttackSaveHp.class,
		PhysicalMute.class,
		PhysicalSoulAttack.class,
		PkCount.class,
		PhysicalAttackWeaponBonus.class,
		ProtectionBlessing.class,
		PullBack.class,
		RandomizeHate.class,
		RebalanceHP.class,
		Recovery.class,
		RefuelAirship.class,
		RegularAttack.class,
		Relax.class,
		ResistSkill.class,
		Restoration.class,
		RestorationRandom.class,
		Resurrection.class,
		ResurrectionSpecial.class,
		Root.class,
		SendSystemMessageToClan.class,
		ServitorShare.class,
		SetSkill.class,
		SetHp.class,
		ShilensBreath.class,
		SilentMove.class,
		SkillTurning.class,
		SkillTurningOverTime.class,
		SoulBlow.class,
		SoulEating.class,
		Sow.class,
		Spoil.class,
		StatByMoveType.class,
		EffectFlag.class,
		StaticDamage.class,
		StealAbnormal.class,
		Summon.class,
		SummonAgathion.class,
		SummonCubic.class,
		SummonHallucination.class,
		SummonNpc.class,
		SummonPet.class,
		SummonTrap.class,
		Sweeper.class,
		Synergy.class,
		TakeCastle.class,
		TakeCastleStart.class,
		TakeFort.class,
		TakeFortStart.class,
		TalismanSlot.class,
		TargetCancel.class,
		TargetMe.class,
		TargetMeProbability.class,
		Teleport.class,
		TeleportToNpc.class,
		TeleportToSummon.class,
		TeleportToTarget.class,
		ThrowUp.class,
		TransferDamage.class,
		TransferHate.class,
		Transformation.class,
		TrapDetect.class,
		TrapRemove.class,
		TriggerSkillByAttack.class,
		TriggerSkillByAvoid.class,
		TriggerSkillByDamage.class,
		TriggerSkillByKill.class,
		TriggerSkillByMagicType.class,
		TriggerSkillBySkill.class,
		TriggerSkillBySkillAttack.class,
		UnsummonAgathion.class,
		UnsummonServitors.class,
		Untargetable.class,
		VampiricAttack.class,
		VisualBuff.class,
		VitalityPointUp.class,
	};
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args)
	{
		for (Class<?> c : EFFECTS)
		{
			if (c == null)
			{
				continue; // Disabled handler
			}
			EffectHandler.getInstance().registerHandler((Class<? extends AbstractEffect>) c);
		}
		
		LOGGER.info("Loaded {} effect handlers.", EffectHandler.getInstance().size());
	}
}
