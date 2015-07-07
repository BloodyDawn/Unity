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

import handlers.effecthandlers.*;

import org.l2junity.gameserver.handler.EffectHandler;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Effect Master handler.
 * @author BiggBoss, Zoey76
 */
public final class EffectMasterHandler
{
	private static final Logger LOGGER = LoggerFactory.getLogger(EffectMasterHandler.class);
	
	private static final Class<?>[] EFFECTS =
	{
		AddHate.class,
		AttackTrait.class,
		Backstab.class,
		Betray.class,
		Blink.class,
		BlinkSwap.class,
		BlockAction.class,
		BlockChat.class,
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
		CancelEffectOnOffense.class,
		ChameleonRest.class,
		ChangeFace.class,
		ChangeFishingMastery.class,
		ChangeHairColor.class,
		ChangeHairStyle.class,
		ClanGate.class,
		ClassChange.class,
		Confuse.class,
		ConsumeBody.class,
		ConvertItem.class,
		CpDamPercent.class,
		CpHeal.class,
		CpHealOverTime.class,
		CpHealPercent.class,
		CrystalGradeModify.class,
		CubicMastery.class,
		DamOverTime.class,
		DamOverTimePercent.class,
		DeathLink.class,
		Debuff.class,
		DebuffBlock.class,
		DefenceTrait.class,
		DeleteHate.class,
		DeleteHateOfMe.class,
		DetectHiddenObjects.class,
		Detection.class,
		DisableTargeting.class,
		Disarm.class,
		Disarmor.class,
		DispelAll.class,
		DispelByCategory.class,
		DispelBySlot.class,
		DispelBySlotProbability.class,
		EnableCloak.class,
		EnemyCharge.class,
		EnergyAttack.class,
		EnlargeAbnormalSlot.class,
		Escape.class,
		EscapeToNpc.class,
		FakeDeath.class,
		FatalBlow.class,
		Fear.class,
		Feed.class,
		Fishing.class,
		Flag.class,
		FlyMove.class,
		FocusEnergy.class,
		FocusMaxEnergy.class,
		FocusSouls.class,
		GetAgro.class,
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
		KnockBack.class,
		KnockDown.class,
		Lethal.class,
		Lucky.class,
		MagicalAbnormalDispelAttack.class,
		MagicalAttack.class,
		MagicalAttackByAbnormal.class,
		MagicalAttackMp.class,
		MagicalDamOverTime.class,
		MagicalSoulAttack.class,
		ManaDamOverTime.class,
		ManaHeal.class,
		ManaHealByLevel.class,
		ManaHealOverTime.class,
		ManaHealPercent.class,
		ModifyVital.class,
		MpConsumePerLevel.class,
		Mute.class,
		NoblesseBless.class,
		OpenChest.class,
		Unsummon.class,
		OpenCommonRecipeBook.class,
		OpenDoor.class,
		OpenDwarfRecipeBook.class,
		Paralyze.class,
		Passive.class,
		Petrification.class,
		Plunder.class,
		PhysicalAttack.class,
		PhysicalAttackHpLink.class,
		PhysicalAttackMute.class,
		PhysicalMute.class,
		PhysicalSoulAttack.class,
		Pumping.class,
		ProtectionBlessing.class,
		PullBack.class,
		RandomizeHate.class,
		RebalanceHP.class,
		Recovery.class,
		Reeling.class,
		RefuelAirship.class,
		RegularAttack.class,
		Relax.class,
		ResetDebuff.class,
		ResistSkill.class,
		Restoration.class,
		RestorationRandom.class,
		Resurrection.class,
		ResurrectionSpecial.class,
		Root.class,
		ServitorShare.class,
		SetSkill.class,
		SetHp.class,
		ShilensBreath.class,
		SilentMove.class,
		SkillTurning.class,
		SkillTurningOverTime.class,
		Sleep.class,
		SoulBlow.class,
		SoulEating.class,
		Sow.class,
		Spoil.class,
		EffectFlag.class,
		StaticDamage.class,
		StealAbnormal.class,
		Stun.class,
		Summon.class,
		SummonAgathion.class,
		SummonCubic.class,
		SummonNpc.class,
		SummonPet.class,
		SummonTrap.class,
		Sweeper.class,
		TakeCastle.class,
		TakeCastleStart.class,
		TakeFort.class,
		TakeFortStart.class,
		TalismanSlot.class,
		TargetCancel.class,
		TargetMe.class,
		TargetMeProbability.class,
		Teleport.class,
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
		TriggerSkillBySkill.class,
		TriggerSkillBySkillAttack.class,
		UnsummonAgathion.class,
		Untargetable.class,
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
