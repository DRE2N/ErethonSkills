package de.erethon.spellbook.spells.warrior;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class ShoutRoar extends AbstractWarriorShout {

    private final EffectData effect = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("RoarDebuff");
    private final int duration = data.getInt("duration", 40);

    public ShoutRoar(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        caster.getWorld().playSound(Sound.sound(org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, Sound.Source.RECORD, 0.8f, 1));
        for (LivingEntity living : caster.getLocation().getNearbyLivingEntities(range)) {
            if (living == caster) continue;
            if (!Spellbook.canAttack(caster, living)) continue;
            living.addEffect(caster, effect, duration, 1);
        }
        return super.onCast();
    }
}
