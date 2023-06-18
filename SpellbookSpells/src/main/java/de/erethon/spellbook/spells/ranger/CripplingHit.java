package de.erethon.spellbook.spells.ranger;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class CripplingHit extends RangerBaseSpell{

    private final int radius = data.getInt("radius", 2);
    private final int nauseaDuration = data.getInt("nauseaDuration", 20);

    public CripplingHit(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onCast() {
        caster.getUsedSpells().put(data, System.currentTimeMillis());
        Vector inFront = caster.getLocation().getDirection().multiply(2);
        Location target = caster.getLocation().add(inFront);
        for (LivingEntity living : target.getNearbyLivingEntities(radius)) {
            if (!Spellbook.canAttack(caster, living)) {
                continue;
            }
            living.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, nauseaDuration, 1));
        }
        return super.onCast();
    }
}
