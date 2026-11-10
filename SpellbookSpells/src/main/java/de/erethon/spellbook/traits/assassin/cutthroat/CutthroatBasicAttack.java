package de.erethon.spellbook.traits.assassin.cutthroat;

import de.erethon.papyrus.PDamageType;
import de.erethon.papyrus.combat.CombatSource;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CutthroatBasicAttack extends SpellTrait implements Listener {

    // Every 3rd attack grants bonus energy and damage. The bonus damage is multiplied by a factor of 1.2.
    // The target is also inflicted with a bleeding effect that lasts between 5 and 10 seconds, scaling with advantage_magical.

    private final int attacksForBonus = data.getInt("attacksForBonus", 3);
    private final int normalEnergy = data.getInt("normalEnergy", 5);
    private final int bonusEnergy = data.getInt("bonusEnergy", 15);
    private final double bonusDamageMultiplier = data.getDouble("bonusDamageMultiplier", 1.2);
    private final int bleedMinDuration = data.getInt("bleedMinDuration", 5);
    private final int bleedMaxDuration = data.getInt("bleedMaxDuration", 10);

    private int currentAttacks = 0;
    private LivingEntity target;
    private final EffectData bleedEffectData = Spellbook.getEffectData("Bleeding");

    public CutthroatBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        super.onAdd();
        caster.setMaxEnergy(100);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    protected void onRemove() {
        HandlerList.unregisterAll(this);
        super.onRemove();
    }

    private boolean isWeaponHit(EntityDamageByEntityEvent event) {
        CombatSource source = event.getCombatSource();
        return event.getDamager() == caster && event.getEntity() instanceof LivingEntity
                && event.getDamageType() == PDamageType.PHYSICAL
                && source != null && source.type() == CombatSource.Type.BASIC_ATTACK;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void empowerThirdHit(EntityDamageByEntityEvent event) {
        if (!isWeaponHit(event) || event.getDamage() <= 0) {
            return;
        }
        int count = event.getEntity() == target ? currentAttacks : 0;
        if (count + 1 >= attacksForBonus) {
            event.setDamage(event.getDamage() * bonusDamageMultiplier);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void confirmHit(EntityDamageByEntityEvent event) {
        if (!isWeaponHit(event) || event.getDamage() <= 0) {
            return;
        }
        LivingEntity hit = (LivingEntity) event.getEntity();
        if (hit != target) {
            target = hit;
            currentAttacks = 0;
        }
        caster.addEnergy(normalEnergy);
        if (++currentAttacks < attacksForBonus) {
            if (currentAttacks == attacksForBonus - 1) {
                caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.7f);
                caster.getWorld().spawnParticle(Particle.CRIT, caster.getEyeLocation(), 3, 0.25, 0.15, 0.25);
            }
            return;
        }
        currentAttacks = 0;
        caster.addEnergy(bonusEnergy);
        caster.getWorld().playSound(hit.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 1.4f);
        hit.getWorld().spawnParticle(Particle.DRIPPING_LAVA, hit.getEyeLocation(), 5, 0.3, 0.4, 0.3);
        hit.addEffect(caster, bleedEffectData, (int) Spellbook.getRangedValue(data, caster, hit,
                Attribute.ADVANTAGE_MAGICAL, bleedMinDuration, bleedMaxDuration, "bleed") * 20, 1);
    }
}
