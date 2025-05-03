package de.erethon.spellbook.spells.assassin.shadow;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.erethon.spellbook.traits.assassin.shadow.ShadowEchoReturnTrait;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class ShadowEcho extends AssassinBaseSpell {

    // Marks a location. The Shadow can return to the marked location by casting the spell again. Upon return, gain Resistance.
    // If a marked target was damaged during the duration of the spell, gain energy.

    private final int energyRestoreOnMarkedDamage = data.getInt("energyRestoreOnMarkedDamage", 25);
    private final int resistanceMinDuration = data.getInt("resistanceMinDuration", 6) * 20;
    private final int resistanceMaxDuration = data.getInt("resistanceMaxDuration", 16) * 20;

    private ShadowEchoReturnTrait echo = null;
    private final TraitData echoTraitData = Bukkit.getServer().getSpellbookAPI().getLibrary().getTraitByID("ShadowEchoReturn");
    private final EffectData resistanceData = Spellbook.getEffectData("Resistance");
    private int visualTicks = 20;

    public ShadowEcho(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        getOrAddEchoTrait();
        if (echo.isWaitingForReturn()) {
            caster.teleport(echo.getReturnLocation());
            caster.addEffect(caster, resistanceData, (int) Spellbook.getRangedValue(data, caster, Attribute.RESISTANCE_MAGICAL, resistanceMinDuration, resistanceMaxDuration,"resistanceDuration"), 1);
            if (echo.hasDamagedMarkedTarget()) {
                caster.setEnergy(caster.getEnergy() + energyRestoreOnMarkedDamage);
            }
            caster.playSound(Sound.sound(org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, Sound.Source.RECORD, 0.8f, 1));
            return false;
        }
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        echo.setReturnLocation(caster.getLocation());
        return super.onCast();
    }

    @Override
    protected void onTick() {
        visualTicks--;
        if (visualTicks <= 0) {
            visualTicks = 20;
            caster.getWorld().spawnParticle(Particle.ASH, echo.getReturnLocation(), 5, 1, 1, 1);
        }
        super.onTick();
    }

    @Override
    protected void onTickFinish() {
        caster.removeTrait(echoTraitData);
        caster.playSound(Sound.sound(org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, Sound.Source.RECORD, 0.8f, 0));
    }

    private void getOrAddEchoTrait() {
        for (SpellTrait trait : caster.getActiveTraits()) {
            if (trait instanceof ShadowEchoReturnTrait echoTrait) {
                echo = echoTrait;
                return;
            }
        }
        if (echo == null) {;
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
        spellAddedPlaceholders.add(Component.text(Spellbook.getRangedValue(data, caster, Attribute.RESISTANCE_MAGICAL, resistanceMinDuration, resistanceMaxDuration,"resistance"), VALUE_COLOR));
        placeholderNames.add("resistanceDuration");
    }
}
