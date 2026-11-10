package de.erethon.spellbook.traits.paladin.inquisitor;

import de.erethon.papyrus.PDamageType;
import de.erethon.papyrus.combat.CombatSource;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellEffect;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Three confirmed spear contacts make a visible sentence, then a spender chooses its payoff. */
public class InquisitorBasicAttack extends SpellTrait implements Listener {

    private final int maximumJudgementStacks = data.getInt("maximumJudgementStacks", 3);
    private final EffectData judgementData = Spellbook.getEffectData("Judgement");

    public InquisitorBasicAttack(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        super.onAdd();
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    protected void onRemove() {
        HandlerList.unregisterAll(this);
        super.onRemove();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConfirmedSpearHit(EntityDamageByEntityEvent event) {
        CombatSource source = event.getCombatSource();
        if (event.getDamager() != caster || !(event.getEntity() instanceof LivingEntity target)
                || !Spellbook.canAttack(caster, target)
                || event.getDamageType() != PDamageType.PHYSICAL
                || source == null || source.type() != CombatSource.Type.BASIC_ATTACK
                || event.getFinalDamage() <= 0 || judgementData == null) return;
        int current = stacks(target);
        if (current >= maximumJudgementStacks) return;
        target.addEffect(caster, judgementData, 120, 1);
        if (current + 1 >= maximumJudgementStacks) {
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_BELL_USE, 0.8f, 0.8f);
            target.getWorld().spawnParticle(Particle.END_ROD, target.getEyeLocation(),
                    12, 0.25, 0.3, 0.25, 0.02);
        }
    }

    private int stacks(LivingEntity target) {
        for (SpellEffect effect : target.getEffects()) {
            if (effect.data == judgementData) return effect.getStacks();
        }
        return 0;
    }
}