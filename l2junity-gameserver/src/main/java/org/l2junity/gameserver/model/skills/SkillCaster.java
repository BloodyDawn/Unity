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
package org.l2junity.gameserver.model.skills;

import static org.l2junity.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.l2junity.Config;
import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.ThreadPoolManager;
import org.l2junity.gameserver.ai.CtrlEvent;
import org.l2junity.gameserver.ai.CtrlIntention;
import org.l2junity.gameserver.data.xml.impl.ActionData;
import org.l2junity.gameserver.datatables.ItemTable;
import org.l2junity.gameserver.enums.ItemSkillType;
import org.l2junity.gameserver.enums.ShotType;
import org.l2junity.gameserver.enums.StatusUpdateType;
import org.l2junity.gameserver.instancemanager.ZoneManager;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Attackable;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.actor.tasks.character.QueuedMagicUseTask;
import org.l2junity.gameserver.model.effects.EffectFlag;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.events.EventDispatcher;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureSkillFinishCast;
import org.l2junity.gameserver.model.events.impl.character.OnCreatureSkillUse;
import org.l2junity.gameserver.model.events.impl.character.npc.OnNpcSkillSee;
import org.l2junity.gameserver.model.events.returns.TerminateReturn;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.model.holders.SkillUseHolder;
import org.l2junity.gameserver.model.items.L2Item;
import org.l2junity.gameserver.model.items.Weapon;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.options.OptionsSkillHolder;
import org.l2junity.gameserver.model.options.OptionsSkillType;
import org.l2junity.gameserver.model.skills.targets.TargetType;
import org.l2junity.gameserver.model.stats.Formulas;
import org.l2junity.gameserver.model.zone.ZoneId;
import org.l2junity.gameserver.model.zone.ZoneRegion;
import org.l2junity.gameserver.network.client.send.ActionFailed;
import org.l2junity.gameserver.network.client.send.ExRotation;
import org.l2junity.gameserver.network.client.send.FlyToLocation;
import org.l2junity.gameserver.network.client.send.MagicSkillCanceld;
import org.l2junity.gameserver.network.client.send.MagicSkillLaunched;
import org.l2junity.gameserver.network.client.send.MagicSkillUse;
import org.l2junity.gameserver.network.client.send.MoveToPawn;
import org.l2junity.gameserver.network.client.send.SetupGauge;
import org.l2junity.gameserver.network.client.send.StatusUpdate;
import org.l2junity.gameserver.network.client.send.SystemMessage;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;
import org.l2junity.gameserver.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Skill Caster implementation.
 * @author Nik
 */
public class SkillCaster implements Runnable
{
	private static final Logger _log = LoggerFactory.getLogger(SkillCaster.class);
	private static final int CASTING_TIME_CAP = 500;
	
	private final Creature _caster;
	private final SkillCastingType _castingType;
	
	private Creature _target; // Main target of the skill casting, when casting finished, any AOE effects will be applied.
	private WorldObject[] _targets; // Array of affected targets, including main target.
	private Skill _skill;
	private ItemInstance _item; // Referenced item either for consumption or something else.
	private boolean _ctrlPressed;
	private boolean _shiftPressed;
	
	private int _castTime;
	private int _reuseDelay;
	private boolean _skillMastery;
	
	private volatile ScheduledFuture<?> _task = null;
	private final AtomicBoolean _skillLaunched;
	private final AtomicBoolean _isCasting;
	
	public SkillCaster(Creature caster, SkillCastingType castingType)
	{
		Objects.requireNonNull(caster);
		Objects.requireNonNull(castingType);
		
		_caster = caster;
		_castingType = castingType;
		_skillLaunched = new AtomicBoolean();
		_isCasting = new AtomicBoolean();
	}
	
	/**
	 * Checks if casting can be prepared for this skill caster and sets the data required for starting the cast.<br>
	 * The casting data can be altered before {@code startCasting()} has been called, for sutuations where you want a customized casting.
	 * @param target the main target
	 * @param skill the skill that its going to be casted towards the target
	 * @param item the item that is going to be consumed or used as a reference
	 * @param ctrlPressed force use the skill
	 * @param shiftPressed don't move while using the skill
	 * @return {@code true} if skill caster is ready to start casting, {@code false} if conditions are not met to start this casting.
	 */
	public boolean prepareCasting(WorldObject target, Skill skill, ItemInstance item, boolean ctrlPressed, boolean shiftPressed)
	{
		Objects.requireNonNull(skill);
		
		// Casting failed due conditions...
		if (!checkUseConditions(_caster, skill))
		{
			return false;
		}
		
		if (!_isCasting.compareAndSet(false, true))
		{
			_log.warn("Character: {} is attempting to cast {} on {} but he is already casting {} on {}!", _caster, skill, target, _skill, _target);
			return false;
		}
		
		// Check if target is valid.
		target = skill.getTarget(_caster, target, ctrlPressed, shiftPressed, false);
		if (target == null)
		{
			_isCasting.set(false);
			return false;
		}
		
		// TODO: Support for item target.
		if (!target.isCreature())
		{
			return false;
		}
		
		// Get ready the casting parameters.
		_target = (Creature) target;
		_skill = skill;
		_item = item;
		_ctrlPressed = ctrlPressed;
		_shiftPressed = shiftPressed;
		_castTime = getCastTime(_caster, skill, item);
		_reuseDelay = _caster.getStat().getReuseTime(skill);
		_skillMastery = Formulas.calcSkillMastery(_caster, skill);
		_skillLaunched.set(_castTime < CASTING_TIME_CAP);
		return true;
	}
	
	/**
	 * Start the casting of the prepared casting info. <br>
	 */
	public void startCasting()
	{
		if (!isCasting())
		{
			_log.warn("Character: {} is starting a cast, but he is not prepared to cast!", _caster);
			return;
		}
		
		// Disable the skill during the re-use delay and create a task EnableSkill with Medium priority to enable it at the end of the re-use delay
		if (_reuseDelay > 10)
		{
			if (_skillMastery)
			{
				_reuseDelay = 100;
				_caster.sendPacket(SystemMessageId.A_SKILL_IS_READY_TO_BE_USED_AGAIN);
			}
			
			if (_reuseDelay > 30000)
			{
				_caster.addTimeStamp(_skill, _reuseDelay);
			}
			else
			{
				_caster.disableSkill(_skill, _reuseDelay);
			}
		}
		
		// Stop movement when casting. Exception are cases where skill doesn't stop movement.
		if (!isSimultaneousType())
		{
			_caster.getAI().clientStopMoving(null);
		}
		
		// Consume the required items.
		if (_skill.getItemConsumeId() > 0)
		{
			if (!_caster.destroyItemByItemId("Consume", _skill.getItemConsumeId(), _skill.getItemConsumeCount(), null, true))
			{
				_caster.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT2);
				stopCasting(true);
				return;
			}
		}
		
		// Reduce talisman mana on skill use
		if ((_skill.getReferenceItemId() > 0) && (ItemTable.getInstance().getTemplate(_skill.getReferenceItemId()).getBodyPart() == L2Item.SLOT_DECO))
		{
			ItemInstance talisman = _caster.getInventory().getItems(i -> i.getId() == _skill.getReferenceItemId(), ItemInstance::isEquipped).stream().findAny().orElse(null);
			if (talisman != null)
			{
				talisman.decreaseMana(false, talisman.useSkillDisTime());
			}
		}
		
		if (_target != _caster)
		{
			// Face the target
			_caster.setHeading(Util.calculateHeadingFrom(_caster, _target));
			_caster.broadcastPacket(new ExRotation(_caster.getObjectId(), _caster.getHeading()));
			
			// Send MoveToPawn packet to trigger Blue Bubbles on target become Red, but don't do it while (double) casting, because that will screw up animation... some fucked up stuff, right?
			if (_caster.isPlayer() && !_caster.isCastingNow())
			{
				_caster.sendPacket(new MoveToPawn(_caster, _target, (int) _caster.calculateDistance(_target, false, false)));
				_caster.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
		
		// Send a Server->Client packet MagicSkillUser with target, displayId, level, skillTime, reuseDelay
		final int actionId = _caster.isSummon() ? ActionData.getInstance().getSkillActionId(_skill.getId()) : -1;
		_caster.broadcastPacket(new MagicSkillUse(_caster, _target, _skill.getDisplayId(), _skill.getDisplayLevel(), _castTime, _reuseDelay, _skill.getReuseDelayGroup(), actionId, _castingType));
		
		// Consume skill initial MP needed for cast.
		int initmpcons = _caster.getStat().getMpInitialConsume(_skill);
		if (initmpcons > 0)
		{
			_caster.getStatus().reduceMp(initmpcons);
			StatusUpdate su = new StatusUpdate(_caster);
			su.addUpdate(StatusUpdateType.CUR_MP, (int) _caster.getCurrentMp());
			_caster.sendPacket(su);
		}
		
		// Send a system message to the player.
		if (_caster.isPlayer() && !_skill.isAbnormalInstant())
		{
			if (_skill.getId() == 2046) // Wolf Collar
			{
				_caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SUMMONING_YOUR_PET));
			}
			else
			{
				_caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_USE_S1).addSkillName(_skill));
			}
		}
		
		// Trigger any skill cast start effects.
		_skill.applyEffectScope(EffectScope.START, new BuffInfo(_caster, _target, _skill, false, _item, null), true, false);
		
		// Casting action is starting...
		_caster.stopEffectsOnAction();
		
		// Show the gauge bar for casting.
		if (_caster.isPlayer())
		{
			_caster.sendPacket(new SetupGauge(_caster.getObjectId(), SetupGauge.BLUE, _castTime));
		}
		
		// Broadcast fly animation if needed. Packet order is after setup gauge.
		if (_skill.getFlyType() != null)
		{
			_caster.broadcastPacket(new FlyToLocation(_caster, _target, _skill.getFlyType()));
		}
		
		// Start channeling if skill is channeling.
		if (_skill.isChanneling() && (_skill.getChannelingSkillId() > 0))
		{
			_caster.getSkillChannelizer().startChanneling(_skill);
		}
		
		// Schedule a thread that will execute 500ms before casting time is over (for animation issues and retail handling).
		_task = ThreadPoolManager.getInstance().scheduleEffect(this, _castTime - CASTING_TIME_CAP);
	}
	
	@Override
	public void run()
	{
		if (!isCasting())
		{
			_log.warn("Character: {} is casting, but casting is false. Skill: {}, Target: {}", _caster, _skill, _target);
			return;
		}
		
		Creature target = _target;
		final WorldObject[] targets;
		Skill skill = _skill;
		ItemInstance item = _item;
		
		try
		{
			// If skill is not launched, pick targets, launch the skill and reschedule skill cast finish.
			if (!_skillLaunched.get())
			{
				_targets = skill.getTargetsAffected(_caster, target).toArray(new WorldObject[0]);
				
				// Finish flying by setting the target location after picking targets.
				if (skill.getFlyType() != null)
				{
					_caster.setXYZ(target.getX(), target.getY(), target.getZ());
				}
				
				_caster.broadcastPacket(new MagicSkillLaunched(_caster, skill.getDisplayId(), skill.getDisplayLevel(), _castingType, _targets));
				_skillLaunched.set(true);
				
				// Reschedule cast finish and wait.
				_task = ThreadPoolManager.getInstance().scheduleEffect(this, CASTING_TIME_CAP);
				return;
			}
			
			// Check if targets have been cached when skill was launched, otherwise pick the list.
			targets = _targets != null ? _targets : skill.getTargetsAffected(_caster, target).toArray(new WorldObject[0]);
			
			// Recharge shots
			_caster.rechargeShots(skill.useSoulShot(), skill.useSpiritShot(), false);
			
			final StatusUpdate su = new StatusUpdate(_caster);
			
			// Consume the required MP or stop casting if not enough.
			double mpConsume = skill.getMpConsume() > 0 ? _caster.getStat().getMpConsume(skill) : 0;
			if (mpConsume > 0)
			{
				if (mpConsume > _caster.getCurrentMp())
				{
					_caster.sendPacket(SystemMessageId.NOT_ENOUGH_MP);
					stopCasting(true, skill, target, item);
					return;
				}
				
				_caster.getStatus().reduceMp(mpConsume);
				su.addUpdate(StatusUpdateType.CUR_MP, (int) _caster.getCurrentMp());
			}
			
			// Consume the required HP or stop casting if not enough.
			double consumeHp = skill.getHpConsume();
			if (consumeHp > 0)
			{
				if (consumeHp >= _caster.getCurrentHp())
				{
					_caster.sendPacket(SystemMessageId.NOT_ENOUGH_HP);
					stopCasting(true, skill, target, item);
					return;
				}
				
				_caster.getStatus().reduceHp(consumeHp, _caster, true);
				su.addUpdate(StatusUpdateType.CUR_HP, (int) _caster.getCurrentHp());
			}
			
			// Send HP/MP consumption packet if any attribute is set.
			if (su.hasUpdates())
			{
				_caster.sendPacket(su);
			}
			
			// Consume Souls if necessary
			if (_caster.isPlayer() && (skill.getMaxSoulConsumeCount() > 0))
			{
				if (!_caster.getActingPlayer().decreaseSouls(skill.getMaxSoulConsumeCount(), skill))
				{
					stopCasting(true, skill, target, item);
					return;
				}
			}
			
			// Launch the magic skill in order to calculate its effects
			try
			{
				// Check if the toggle skill effects are already in progress on the L2Character
				if (skill.isToggle() && _caster.isAffectedBySkill(skill.getId()))
				{
					stopCasting(true, skill, target, item);
					return;
				}
				
				// Initial checks
				for (WorldObject obj : targets)
				{
					if ((obj == null) || !obj.isCreature())
					{
						continue;
					}
					
					final Creature creature = (Creature) obj;
					
					// Check raid monster/minion attack and check buffing characters who attack raid monsters. Raid is still affected by skills.
					if (!Config.RAID_DISABLE_CURSE && creature.isRaid() && creature.giveRaidCurse() && (_caster.getLevel() >= (creature.getLevel() + 9)))
					{
						if (skill.isBad() || ((creature.getTarget() == _caster) && ((Attackable) creature).getAggroList().containsKey(_caster)))
						{
							// Skills such as Summon Battle Scar too can trigger magic silence.
							final CommonSkill curse = skill.isBad() ? CommonSkill.RAID_CURSE2 : CommonSkill.RAID_CURSE;
							Skill curseSkill = curse.getSkill();
							if (curseSkill != null)
							{
								curseSkill.applyEffects(creature, _caster);
							}
						}
					}
					
					// Static skills not trigger any chance skills
					if (!skill.isStatic())
					{
						Weapon activeWeapon = _caster.getActiveWeaponItem();
						// Launch weapon Special ability skill effect if available
						if ((activeWeapon != null) && !creature.isDead())
						{
							activeWeapon.applyConditionalSkills(_caster, creature, skill, ItemSkillType.ON_MAGIC_SKILL);
						}
						
						if (_caster.hasTriggerSkills())
						{
							for (OptionsSkillHolder holder : _caster.getTriggerSkills().values())
							{
								if ((skill.isMagic() && (holder.getSkillType() == OptionsSkillType.MAGIC)) || (skill.isPhysical() && (holder.getSkillType() == OptionsSkillType.ATTACK)))
								{
									if (Rnd.get(100) < holder.getChance())
									{
										_caster.makeTriggerCast(holder.getSkill(), creature, false);
									}
								}
							}
						}
					}
				}
				
				// Launch the magic skill and calculate its effects
				skill.activateSkill(_caster, item, targets);
				
				PlayerInstance player = _caster.getActingPlayer();
				if (player != null)
				{
					for (WorldObject obj : targets)
					{
						if (!(obj instanceof Creature))
						{
							continue;
						}
						
						if (skill.isBad())
						{
							if (obj.isPlayable())
							{
								// Update pvpflag.
								player.updatePvPStatus((Creature) obj);
								
								if (obj.isSummon())
								{
									((Summon) obj).updateAndBroadcastStatus(1);
								}
							}
							else if (obj.isAttackable())
							{
								// Add hate to the attackable, and put it in the attack list.
								((Attackable) obj).addDamageHate(_caster, 0, -skill.getEffectPoint());
								((Creature) obj).addAttackerToAttackByList(_caster);
							}
							
							// notify target AI about the attack
							if (((Creature) obj).hasAI() && !skill.hasEffectType(L2EffectType.HATE))
							{
								((Creature) obj).getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, _caster);
							}
						}
						else if (obj.isMonster() || (obj.isPlayable() && ((obj.getActingPlayer().getPvpFlag() > 0) || (obj.getActingPlayer().getReputation() < 0))))
						{
							// Supporting players or monsters result in pvpflag.
							player.updatePvPStatus();
						}
					}
					
					// Mobs in range 1000 see spell
					World.getInstance().forEachVisibleObjectInRange(player, Npc.class, 1000, npcMob ->
					{
						EventDispatcher.getInstance().notifyEventAsync(new OnNpcSkillSee(npcMob, player, skill, _caster.isSummon(), targets), npcMob);
						
						// On Skill See logic
						if (npcMob.isAttackable())
						{
							final Attackable attackable = (Attackable) npcMob;
							
							if (skill.getEffectPoint() > 0)
							{
								if (attackable.hasAI() && (attackable.getAI().getIntention() == AI_INTENTION_ATTACK))
								{
									WorldObject npcTarget = attackable.getTarget();
									for (WorldObject skillTarget : targets)
									{
										if ((npcTarget == skillTarget) || (npcMob == skillTarget))
										{
											Creature originalCaster = _caster.isSummon() ? _caster : player;
											attackable.addDamageHate(originalCaster, 0, (skill.getEffectPoint() * 150) / (attackable.getLevel() + 7));
										}
									}
								}
							}
						}
					});
				}
			}
			catch (Exception e)
			{
				_log.warn(_caster + " callSkill() failed.", e);
			}
			
			EventDispatcher.getInstance().notifyEvent(new OnCreatureSkillFinishCast(_caster, target, skill, skill.isWithoutAction()), _caster);
			
			// Notify DP Scripts
			_caster.notifyQuestEventSkillFinished(skill, target);
			
			// On each repeat recharge shots before cast.
			_caster.rechargeShots(skill.useSoulShot(), skill.useSpiritShot(), false);
			
			stopCasting(false, skill, target, item);
		}
		catch (Exception e)
		{
			stopCasting(false, skill, target, item);
			_log.warn("Error while casting skill: " + skill + " caster: " + _caster + " target: " + target, e);
		}
	}
	
	/**
	 * Stops this casting and cleans all cast parameters.<br>
	 * @param aborted if {@code true}, server will send packets to the player, notifying him that the skill has been aborted.
	 * @param references used for logging purposes.
	 */
	public void stopCasting(boolean aborted, Object... references)
	{
		// Verify for same status.
		if (!isCasting())
		{
			_log.warn("Character: " + _caster + " is attempting to stop casting skill {} but he is not casting!", _skill, new IllegalStateException(Arrays.toString(references)));
		}
		
		// Cancel the task and unset it.
		if (_task != null)
		{
			_task.cancel(true);
			_task = null;
		}
		
		if ((_skill != null) && (_caster != null) && !isSimultaneousType())
		{
			// Attack target after skill use
			if ((_skill.nextActionIsAttack()) && (_caster.getTarget() instanceof Creature) && (_caster.getTarget() != _caster) && (_target != null) && (_caster.getTarget() == _target) && _target.canBeAttacked())
			{
				if ((_caster.getAI().getNextIntention() == null) || (_caster.getAI().getNextIntention().getCtrlIntention() != CtrlIntention.AI_INTENTION_MOVE_TO))
				{
					_caster.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, _target);
				}
			}
			
			if (_skill.isBad() && (_skill.getTargetType() != TargetType.DOOR_TREASURE))
			{
				_caster.getAI().clientStartAutoAttack();
			}
			
			// If character is a player, then wipe their current cast state and check if a skill is queued.
			// If there is a queued skill, launch it and wipe the queue.
			if (_caster.isPlayer())
			{
				PlayerInstance currPlayer = _caster.getActingPlayer();
				SkillUseHolder queuedSkill = currPlayer.getQueuedSkill();
				
				if (queuedSkill != null)
				{
					currPlayer.setQueuedSkill(null, false, false);
					
					// DON'T USE : Recursive call to useMagic() method
					// currPlayer.useMagic(queuedSkill.getSkill(), queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed());
					// TODO: From UnAfraid's item reference here is set _item, but I don't think it should be this casting's item.
					ThreadPoolManager.getInstance().executeGeneral(new QueuedMagicUseTask(currPlayer, queuedSkill.getSkill(), _item, queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed()));
				}
			}
			
			if (_caster.isChanneling())
			{
				_caster.getSkillChannelizer().stopChanneling();
			}
			
			// Notify the AI of the L2Character with EVT_FINISH_CASTING
			_caster.getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING);
			
			if (aborted)
			{
				_caster.broadcastPacket(new MagicSkillCanceld(_caster.getObjectId())); // broadcast packet to stop animations client-side
				_caster.sendPacket(ActionFailed.get(_castingType)); // send an "action failed" packet to the caster
			}
		}
		
		// Cleanup values and allow this SkillCaster to be used again.
		_target = null;
		_targets = null;
		_skill = null;
		_item = null;
		_ctrlPressed = false;
		_shiftPressed = false;
		_castTime = 0;
		_reuseDelay = 0;
		_skillMastery = false;
		_skillLaunched.set(false);
		
		if (!_isCasting.compareAndSet(true, false))
		{
			_log.warn("Character: {} is finishing cast, but he has already finished.", _caster);
		}
	}
	
	/**
	 * @return the skill that has been prepared for casting.
	 */
	public Skill getSkill()
	{
		return _skill;
	}
	
	/**
	 * @return the item that has been used in this casting.
	 */
	public ItemInstance getItem()
	{
		return _item;
	}
	
	/**
	 * @return {@code true} if this casting is forced attack.
	 */
	public boolean isCtrlPressed()
	{
		return _ctrlPressed;
	}
	
	/**
	 * @return {@code true} if this casting is attack without moving.
	 */
	public boolean isShiftPressed()
	{
		return _shiftPressed;
	}
	
	/**
	 * @return if this caster has been prepared, currently is casting and hasn't finished while casting process.
	 */
	public boolean isCasting()
	{
		return _isCasting.get();
	}
	
	/**
	 * @return {@code !isCasting()} which is useful for lambda expressions such as {@code SkillCaster::isNotCasting}
	 */
	public boolean isNotCasting()
	{
		return !_isCasting.get();
	}
	
	/**
	 * @return {@code true} if casting can be aborted through regular means such as cast break while being attacked or while cancelling target, {@code false} otherwise.
	 */
	public boolean canAbortCast()
	{
		return !_skillLaunched.get();
	}
	
	/**
	 * @return the type of this caster, which also defines the casting display bar on the player.
	 */
	public SkillCastingType getCastingType()
	{
		return _castingType;
	}
	
	public boolean isNormalType()
	{
		return (_castingType == SkillCastingType.NORMAL) || (_castingType == SkillCastingType.NORMAL_SECOND);
	}
	
	public boolean isSimultaneousType()
	{
		return _castingType == SkillCastingType.SIMULTANEOUS;
	}
	
	public void setCtrlPressed(boolean ctrlPressed)
	{
		_ctrlPressed = ctrlPressed;
	}
	
	public void setShiftPressed(boolean shiftPressed)
	{
		_shiftPressed = shiftPressed;
	}
	
	public void setCastTime(int castTime)
	{
		_castTime = castTime;
	}
	
	public void setReuseDelay(int reuseDelay)
	{
		_reuseDelay = reuseDelay;
	}
	
	public void setSkillMastery(boolean skillMastery)
	{
		_skillMastery = skillMastery;
	}
	
	public static boolean checkUseConditions(Creature caster, Skill skill)
	{
		if (caster == null)
		{
			return false;
		}
		
		if ((skill == null) || caster.isSkillDisabled(skill) || (((skill.getFlyRadius() > 0) || (skill.getFlyType() != null)) && caster.isMovementDisabled()))
		{
			caster.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		final TerminateReturn term = EventDispatcher.getInstance().notifyEvent(new OnCreatureSkillUse(caster, skill, skill.isWithoutAction()), caster, TerminateReturn.class);
		if ((term != null) && term.terminate())
		{
			caster.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if creature is already casting
		if (caster.isCastingNow(SkillCaster::isNormalType))
		{
			// Check if unable to double cast or both casting slots are taken.
			if (!caster.isAffected(EffectFlag.DOUBLE_CAST) || !skill.canDoubleCast() || (caster.getSkillCaster(SkillCaster::isNotCasting, SkillCaster::isNormalType) == null))
			{
				caster.sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		// Check if the caster has enough MP
		if (caster.getCurrentMp() < (caster.getStat().getMpConsume(skill) + caster.getStat().getMpInitialConsume(skill)))
		{
			caster.sendPacket(SystemMessageId.NOT_ENOUGH_MP);
			caster.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the caster has enough HP
		if (caster.getCurrentHp() <= skill.getHpConsume())
		{
			caster.sendPacket(SystemMessageId.NOT_ENOUGH_HP);
			caster.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Skill mute checks.
		if (!skill.isStatic())
		{
			// Check if the skill is a magic spell and if the L2Character is not muted
			if (skill.isMagic())
			{
				if (caster.isMuted())
				{
					caster.sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
			else
			{
				// Check if the skill is physical and if the L2Character is not physical_muted
				if (caster.isPhysicalMuted())
				{
					caster.sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
		}
		
		// prevent casting signets to peace zone
		if (skill.isChanneling() && (skill.getChannelingSkillId() > 0))
		{
			final ZoneRegion zoneRegion = ZoneManager.getInstance().getRegion(caster);
			boolean canCast = true;
			if ((skill.getTargetType() == TargetType.GROUND) && caster.isPlayer())
			{
				Location wp = caster.getActingPlayer().getCurrentSkillWorldPosition();
				if (!zoneRegion.checkEffectRangeInsidePeaceZone(skill, wp.getX(), wp.getY(), wp.getZ()))
				{
					canCast = false;
				}
			}
			else if (!zoneRegion.checkEffectRangeInsidePeaceZone(skill, caster.getX(), caster.getY(), caster.getZ()))
			{
				canCast = false;
			}
			if (!canCast)
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
				sm.addSkillName(skill);
				caster.sendPacket(sm);
				return false;
			}
		}
		
		// Check if the caster's weapon is limited to use only its own skills
		final Weapon weapon = caster.getActiveWeaponItem();
		if ((weapon != null) && weapon.useWeaponSkillsOnly() && !caster.isGM() && (weapon.getSkills(ItemSkillType.NORMAL) != null))
		{
			boolean found = false;
			for (SkillHolder sh : weapon.getSkills(ItemSkillType.NORMAL))
			{
				if (sh.getSkillId() == skill.getId())
				{
					found = true;
				}
			}
			
			if (!found)
			{
				if (caster.getActingPlayer() != null)
				{
					caster.sendPacket(SystemMessageId.THAT_WEAPON_CANNOT_USE_ANY_OTHER_SKILL_EXCEPT_THE_WEAPON_S_SKILL);
				}
				return false;
			}
		}
		
		// Check if the spell consumes an Item
		// TODO: combine check and consume
		if ((skill.getItemConsumeId() > 0) && (caster.getInventory() != null))
		{
			// Get the L2ItemInstance consumed by the spell
			ItemInstance requiredItems = caster.getInventory().getItemByItemId(skill.getItemConsumeId());
			
			// Check if the caster owns enough consumed Item to cast
			if ((requiredItems == null) || (requiredItems.getCount() < skill.getItemConsumeCount()))
			{
				// Checked: when a summon skill failed, server show required consume item count
				if (skill.hasEffectType(L2EffectType.SUMMON))
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.SUMMONING_A_SERVITOR_COSTS_S2_S1);
					sm.addItemName(skill.getItemConsumeId());
					sm.addInt(skill.getItemConsumeCount());
					caster.sendPacket(sm);
				}
				else
				{
					// Send a System Message to the caster
					caster.sendPacket(SystemMessageId.THERE_ARE_NOT_ENOUGH_NECESSARY_ITEMS_TO_USE_THE_SKILL);
				}
				return false;
			}
		}
		
		if (caster.isPlayer())
		{
			PlayerInstance player = caster.getActingPlayer();
			if (player.inObserverMode())
			{
				return false;
			}
			
			if (player.isInOlympiadMode() && skill.isBlockedInOlympiad())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THAT_SKILL_IN_A_OLYMPIAD_MATCH);
				return false;
			}
			
			if (player.isInsideZone(ZoneId.SAYUNE))
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_USE_SKILLS_IN_THE_CORRESPONDING_REGION);
				return false;
			}
			
			// Check if not in AirShip
			if (player.isInAirShip() && !skill.hasEffectType(L2EffectType.REFUEL_AIRSHIP))
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
				sm.addSkillName(skill);
				player.sendPacket(sm);
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Calculates the time required for this skill to be cast.
	 * @param caster the creature that is requesting the calculation.
	 * @param skill the skill from which casting time will be calculated.
	 * @param item the item that has been used for this skill cast.
	 * @return the time in milliseconds required for this skill to be casted.
	 */
	public static int getCastTime(Creature caster, Skill skill, ItemInstance item)
	{
		if ((item != null) && (item.getItem().hasImmediateEffect() || item.getItem().hasExImmediateEffect()))
		{
			return 0;
		}
		
		// Get the Base Casting Time of the Skills.
		int skillTime = (skill.getHitTime() + skill.getCoolTime());
		
		if (!skill.isChanneling() || (skill.getChannelingSkillId() == 0))
		{
			// Calculate the Casting Time of the "Non-Static" Skills (with caster PAtk/MAtkSpd).
			if (!skill.isStatic())
			{
				skillTime = Formulas.calcAtkSpd(caster, skill, skillTime);
			}
			// Calculate the Casting Time of Magic Skills (reduced in 40% if using SPS/BSPS)
			if (skill.isMagic() && (caster.isChargedShot(ShotType.SPIRITSHOTS) || caster.isChargedShot(ShotType.BLESSED_SPIRITSHOTS)))
			{
				skillTime = (int) (0.6 * skillTime);
			}
		}
		
		// Client have casting time cap for skills that is 500ms. Skills cannot go lower than 500ms cast time, unless their original cast time is such.
		if (skill.getHitTime() > CASTING_TIME_CAP)
		{
			skillTime = Math.max(CASTING_TIME_CAP, skillTime);
		}
		
		return skillTime;
	}
}
