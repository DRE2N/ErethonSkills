package de.erethon.spellbook.spells.assassin.shadow;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.erethon.spellbook.traits.assassin.shadow.ShadowEchoReturnTrait;
import io.papermc.paper.entity.TeleportFlag;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerTeleportEvent;

public class ShadowEcho extends ShadowBaseSpell {

    private final int energyRestoreOnMarkedDamage = data.getInt("energyRestoreOnMarkedDamage", 25);
    private final int resistanceMinDuration = data.getInt("resistanceMinDuration", 6) * 20;
    private final int resistanceMaxDuration = data.getInt("resistanceMaxDuration", 16) * 20;

    private ShadowEchoReturnTrait echo = null;
    private final TraitData echoTraitData = Bukkit.getServer().getSpellbookAPI().getLibrary().getTraitByID("ShadowEchoReturnTrait");
    private final EffectData resistanceData = Spellbook.getEffectData("Resistance");
    private int visualTicks = 10;
    private AoE shadowPortal = null;

    public ShadowEcho(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        getOrAddEchoTrait();
        if (echo.isWaitingForReturn()) {
            Location returnLoc = echo.getReturnLocation();

            caster.getWorld().spawnParticle(Particle.PORTAL, caster.getLocation().add(0, 1, 0), 30, 1, 1, 1, 1);
            caster.getWorld().spawnParticle(Particle.PORTAL, returnLoc.add(0, 1, 0), 30, 1, 1, 1, 1);
            caster.getWorld().spawnParticle(Particle.DUST, caster.getLocation().add(0, 1, 0), 20, 0.8, 0.8, 0.8, 0.1, new Particle.DustOptions(Color.BLACK, 1.5f));

            caster.getWorld().playSound(caster.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
            caster.getWorld().playSound(returnLoc, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);

            caster.teleport(returnLoc, PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);
            caster.addEffect(caster, resistanceData, (int) Spellbook.getRangedValue(data, caster, Attribute.RESISTANCE_MAGICAL, resistanceMinDuration, resistanceMaxDuration,"resistanceDuration"), 1);

            if (echo.hasDamagedMarkedTarget()) {
                caster.setEnergy(caster.getEnergy() + energyRestoreOnMarkedDamage);
                caster.getWorld().spawnParticle(Particle.ENCHANT, caster.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 1);
            }

            if (shadowPortal != null) {
                shadowPortal.revertBlockChanges();
                shadowPortal = null;
            }

            caster.removeTrait(echoTraitData);
            return false;
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        Location markLoc = caster.getLocation().clone();
        echo.setReturnLocation(markLoc);

        caster.getWorld().playSound(markLoc, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.5f);
        caster.getWorld().playSound(markLoc, org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.8f);

        caster.getWorld().spawnParticle(Particle.PORTAL, markLoc.add(0, 1, 0), 20, 0.8, 0.5, 0.8, 0.5);
        caster.getWorld().spawnParticle(Particle.DUST, markLoc.add(0, 0.5, 0), 15, 1, 0.2, 1, 0.1, new Particle.DustOptions(Color.PURPLE, 1.2f));

        shadowPortal = createCircularAoE(markLoc, 2.0, 0.5, keepAliveTicks)
            .addBlockChange(Material.BLACK_CONCRETE)
            .sendBlockChanges();

        return super.onCast();
    }

    @Override
    protected void onTick() {
        visualTicks--;
        if (visualTicks <= 0) {
            visualTicks = 10;
            Location portalLoc = echo.getReturnLocation();
            portalLoc.getWorld().spawnParticle(Particle.PORTAL, portalLoc.clone().add(0, 1, 0), 8, 0.8, 0.3, 0.8, 0.2);
            portalLoc.getWorld().spawnParticle(Particle.DUST, portalLoc.clone().add(0, 0.5, 0), 3, 0.5, 0.1, 0.5, 0, new Particle.DustOptions(Color.PURPLE, 0.8f));
            portalLoc.getWorld().spawnParticle(Particle.WHITE_ASH, portalLoc.clone().add(0, 2, 0), 2, 1, 0.5, 1, 0.01);
        }
        super.onTick();
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        caster.removeTrait(echoTraitData);
        if (shadowPortal != null) {
            shadowPortal.revertBlockChanges();
        }

        Location portalLoc = echo.getReturnLocation();
        portalLoc.getWorld().spawnParticle(Particle.SMOKE, portalLoc.clone().add(0, 1, 0), 15, 1, 0.5, 1, 0.1);
        portalLoc.getWorld().playSound(portalLoc, org.bukkit.Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 1.5f);
    }

    private void getOrAddEchoTrait() {
        for (SpellTrait trait : caster.getActiveTraits()) {
            if (trait instanceof ShadowEchoReturnTrait echoTrait) {
                echo = echoTrait;
                return;
            }
        }
        if (echo == null) {
            caster.addTrait(echoTraitData);
            for (SpellTrait trait : caster.getActiveTraits()) {
                if (trait instanceof ShadowEchoReturnTrait echoTrait) {
                    echo = echoTrait;
                    break;
                }
            }
        }
    }

    @Override
    protected void addSpellPlaceholders() {
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.RESISTANCE_MAGICAL, resistanceMinDuration, resistanceMaxDuration,"resistanceDuration"), VALUE_COLOR));
        placeholderNames.add("resistanceDuration");
    }
}
