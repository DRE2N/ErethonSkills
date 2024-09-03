package de.erethon.spellbook.spells;

import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.Targeted;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public abstract class SpellbookBaseSpell extends SpellbookSpell implements Targeted {

    public static final int CD_PLACEHOLDER = 0;
    public static TextColor VALUE_COLOR = TextColor.fromCSSHexString("#027DCA");
    public static TextColor ATTR_PHYSICAL_COLOR = TextColor.fromCSSHexString("#f02607");
    public static TextColor ATTR_MAGIC_COLOR = TextColor.fromCSSHexString("#0fdcfa");
    public static TextColor ATTR_HEALING_POWER_COLOR = TextColor.fromCSSHexString("#32ac21");

    protected List<Component> spellAddedPlaceholders = new ArrayList<>();

    public SpellbookBaseSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onCast() {
        return super.onCast();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        List<Component> placeholders = new ArrayList<>();
        placeholders.add(Component.text(data.getCooldown(), VALUE_COLOR));
        placeholders.addAll(spellAddedPlaceholders);
        return placeholders;
    }
}
