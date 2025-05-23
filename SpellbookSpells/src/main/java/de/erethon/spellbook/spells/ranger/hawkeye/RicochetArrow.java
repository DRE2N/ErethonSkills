package de.erethon.spellbook.spells.ranger.hawkeye;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.ranger.RangerBaseSpell;
import de.erethon.spellbook.utils.RangerUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.List;

public class RicochetArrow extends RangerBaseSpell implements Listener {

    // The Hawkeye sends a projectile that ricochets off of the first target and hits a second target.

    private int ricochetRange = data.getInt("ricochetRange", 7);
    public int maxRicochets = data.getInt("maxRicochets", 8);
    public double damageReductionPerRicochet = data.getDouble("damageReductionPerRicochet", 1);
    private final int projectileSpeed = data.getInt("projectileSpeed", 2);
    private final int bonusRicochets = data.getInt("bonusRicochets", 2);
    private final int bonusRicochetRange = data.getInt("bonusRicochetRange", 2);
    private final int weaknessDurationMin = data.getInt("weaknessDurationMin", 60);
    private final int weaknessDurationMax = data.getInt("weaknessDurationMax", 240);

    private final EffectData weaknessEffect = Spellbook.getEffectData("Weakness");

    private Projectile initialProjectile;
    private int ricochets = 0;

    public RicochetArrow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    protected boolean onPrecast() {
        return lookForTarget(true);
    }

    @Override
    public boolean onCast() {
        caster.getUsedSpells().put(data, System.currentTimeMillis());
        initialProjectile = RangerUtils.sendProjectile(caster, target, caster,  projectileSpeed, Spellbook.getVariedAttributeBasedDamage(data, caster, target, false, Attribute.ADVANTAGE_MAGICAL), PDamageType.MAGIC);
        if (inFlowState()) {
            maxRicochets *= 2;
            ricochetRange *= 2;
            removeFlow();
        }
        return true;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() != initialProjectile) {
            return;
        }
        boolean wasInFlowState = inFlowState();
        if (inFlowState()) {
            maxRicochets += bonusRicochets;
            ricochetRange += bonusRicochetRange;
            removeFlow();
        }
        for (LivingEntity living : event.getHitEntity().getLocation().getNearbyLivingEntities(ricochetRange)) {
            if (living == caster || living == event.getHitEntity()) {
                continue;
            }
            if (ricochets >= maxRicochets) {
                return;
            }
            RangerUtils.sendProjectile((LivingEntity) event.getHitEntity(), living, caster,  projectileSpeed,
                    Spellbook.getVariedAttributeBasedDamage(data, caster, target, false, Attribute.ADVANTAGE_MAGICAL) - (ricochets * damageReductionPerRicochet) , PDamageType.MAGIC);
            if (wasInFlowState) {
                int weaknessDuration = (int) Spellbook.getRangedValue(data, caster, target, Attribute.ADVANTAGE_MAGICAL, weaknessDurationMin, weaknessDurationMax, "weaknessDuration");
                living.addEffect(caster, weaknessEffect, weaknessDuration, 1);
            }
            ricochets++;
        }
    }

    @Override
    protected void cleanup() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(ricochetRange, VALUE_COLOR));
        placeholderNames.add("ricochetRange");
        spellAddedPlaceholders.add(Component.text(maxRicochets, VALUE_COLOR));
        placeholderNames.add("maxRicochets");
        spellAddedPlaceholders.add(Component.text(damageReductionPerRicochet, VALUE_COLOR));
        placeholderNames.add("damageReductionPerRicochet");
        spellAddedPlaceholders.add(Component.text(projectileSpeed, VALUE_COLOR));
        placeholderNames.add("projectileSpeed");
        return super.getPlaceholders(c);
    }

}
