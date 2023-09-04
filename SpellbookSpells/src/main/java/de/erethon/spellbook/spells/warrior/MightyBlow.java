package de.erethon.spellbook.spells.warrior;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class MightyBlow extends WarriorBaseSpell {

    private final double rangeMultiplier = data.getInt("rangeMultiplier", 2);
    private final int radius = data.getInt("radius", 3);
    private final double pushStrength = data.getDouble("pushStrength", 2);

    public MightyBlow(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onCast() {
        Location location = caster.getLocation();
        Vector inFront = location.toVector().add(location.getDirection().multiply(rangeMultiplier));
        inFront.setY(location.getY());
        caster.playSound(Sound.sound(org.bukkit.Sound.ITEM_FIRECHARGE_USE, Sound.Source.RECORD, 1, 1));
        inFront.toLocation(caster.getWorld()).getNearbyLivingEntities(radius).forEach(entity -> {
            if (entity == caster) return;
            if (!Spellbook.canAttack(caster, entity)) return;
            entity.setVelocity(location.getDirection().multiply(pushStrength));
            entity.playSound(Sound.sound(org.bukkit.Sound.ITEM_FIRECHARGE_USE, Sound.Source.RECORD, 1, 1));
        });
        return super.onCast();
    }
}
