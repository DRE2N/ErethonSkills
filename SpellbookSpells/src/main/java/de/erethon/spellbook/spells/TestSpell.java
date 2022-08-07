package de.erethon.spellbook.spells;

import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.utils.ItemProjectile;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

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
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            ItemProjectile projectile = new ItemProjectile(new ItemStack(Material.DIRT), caster.getEyeLocation().getX(), caster.getEyeLocation().getY(), caster.getEyeLocation().getZ(), caster.getWorld(), this);
            projectile.shoot(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5, 1.5f, 0);
        }
        return true;
    }

    @Override
    protected void onAfterCast() {
        caster.setCooldown(data);
    }

    @Override
    protected void onTick() {
    }

    @Override
    protected void onTickFinish() {
    }
}
