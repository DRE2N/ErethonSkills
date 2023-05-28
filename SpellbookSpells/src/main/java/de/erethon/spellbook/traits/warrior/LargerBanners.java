package de.erethon.spellbook.traits.warrior;

import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.spells.warrior.banners.WarBanner;
import org.bukkit.entity.LivingEntity;

public class LargerBanners extends SpellTrait {

    public LargerBanners(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onSpellPreCast(SpellbookSpell spell) {
        if (spell instanceof WarBanner banner) {
            banner.radius += data.getInt("radiusBonus", 2);
        }
    }
}
