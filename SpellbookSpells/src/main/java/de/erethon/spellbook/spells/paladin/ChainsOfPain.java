package de.erethon.spellbook.spells.paladin;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCastEvent;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.erethon.spellbook.spells.ranger.RangerBaseSpell;
import de.erethon.spellbook.spells.warrior.WarriorBaseSpell;
import de.erethon.spellbook.utils.SpellbookBaseSpell;
import de.erethon.spellbook.utils.Targeted;
import de.slikey.effectlib.effect.LineEffect;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.lang.reflect.InvocationTargetException;

public class ChainsOfPain extends PaladinBaseSpell implements Listener {

    private final double maxDistance = data.getDouble("maxDistance", 7);
    private final double lookForSecondTargetRange = data.getDouble("lookForSecondTargetRange", 5);
    private final EffectData stun = Spellbook.getEffectData("Stun");
    private final int stunDuration = data.getInt("stunDuration", 100);

    private LivingEntity secondTarget;
    private boolean linkActive = false;
    private LineEffect effect;

    public ChainsOfPain(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = spellData.getInt("duration", 1200);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    protected boolean onPrecast() {
        if (!lookForTarget(data.getInt("range", 16))) return false;
        for (LivingEntity living : target.getLocation().getNearbyLivingEntities(lookForSecondTargetRange)) {
            if (living == caster) continue;
            if (!Spellbook.canAttack(caster, living)) continue;
            secondTarget = living;
            break;
        }
        if (secondTarget == null) {
            return false;
        }
        return super.onPrecast();
    }


    @Override
    public boolean onCast() {
        linkActive = true;
        effect = new LineEffect(Spellbook.getInstance().getEffectManager());
        effect.setLocation(caster.getLocation().add(0, -1.5, 0));
        effect.setTargetLocation(target.getLocation().add(0, -1.5, 0));
        effect.particle = Particle.REDSTONE;
        effect.color = Color.RED;
        effect.particleSize = 0.3f;
        effect.start();
        return super.onCast();
    }

    @EventHandler
    public void onSpellCast(SpellCastEvent event) {
        SpellbookSpell activeSpell = event.getActiveSpell();
        LivingEntity eventCaster = event.getCaster();
        if (!Spellbook.canAttack(caster, eventCaster)) return;
        // TODO: Check if this actually works
        if (activeSpell instanceof SpellbookBaseSpell spell) {
            if (spell.getTarget() != null && spell.getTarget() == target) {
                try {
                    SpellbookBaseSpell newSpell = spell.getClass().getDeclaredConstructor(LivingEntity.class, SpellData.class).newInstance(eventCaster, spell.getData());
                    ((Targeted) newSpell).setTarget(secondTarget);
                    newSpell.onCast(); // let's hope no spell _requires_ onPrecast to work. We don't want cooldown or mana costs to be applied here.
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    protected void onTick() {
        if (caster.getLocation().distanceSquared(target.getLocation()) > maxDistance * maxDistance) {
            linkActive = false;
            target.addEffect(caster, stun, stunDuration, 1);
            secondTarget.addEffect(caster, stun, stunDuration, 1);
            interrupt();
            return;
        }
        effect.setLocation(caster.getLocation().add(0, -1.5, 0));
        effect.setTargetLocation(target.getLocation().add(0, -1.5, 0));
    }

    @Override
    protected void cleanup() {
        if (effect == null) return;
        effect.cancel();
    }
}
