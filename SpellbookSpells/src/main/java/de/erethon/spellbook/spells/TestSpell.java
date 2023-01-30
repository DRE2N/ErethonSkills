package de.erethon.spellbook.spells;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.utils.NMSUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.network.chat.Component;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;


public class TestSpell extends SpellbookSpell {

    public TestSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return true;
    }

    @Override
    protected boolean onCast() {
        ArmorStand armorstand = caster.getWorld().spawn(caster.getLocation(), ArmorStand.class);
        armorstand.customName(MiniMessage.miniMessage().deserialize("<lang:block.minecraft.diamond_block>"));
        armorstand.setCustomNameVisible(true);
        return true;
    }

    @Override
    protected void onAfterCast() {
        caster.setCooldown(data);
    }


    @Override
    protected void onTickFinish() {
    }
}
