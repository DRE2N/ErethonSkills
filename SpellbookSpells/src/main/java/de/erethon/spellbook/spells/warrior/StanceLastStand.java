package de.erethon.spellbook.spells.warrior;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.Display;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.List;

public class StanceLastStand extends AbstractWarriorStance {

    private double damageReduction = data.getDouble("damageMultiplier", 0.7);
    private double attackReduction = data.getDouble("attackMultiplier", 0.2);
    private ItemDisplay display;
    private Transformation transformation = new Transformation(new Vector3f(0, 0, 1), new AxisAngle4f(0, 0, 0, 0), new Vector3f(1, 1, 1), new AxisAngle4f(0, 0, 1, 0));

    public StanceLastStand(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected void onTick() {
        super.onTick();
        if (display != null) {
            display.setRotation(display.getLocation().getYaw() + 10, display.getLocation().getPitch());
        }
    }

    @Override
    public boolean onCast(SpellbookSpell spell) {
        display = caster.getWorld().spawn(caster.getLocation(), ItemDisplay.class, itemDisplay -> {
            itemDisplay.setItemStack(new ItemStack(Material.SHIELD));
            itemDisplay.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIRSTPERSON_RIGHTHAND);
            itemDisplay.setTransformation(transformation);
        });
        caster.addPassenger(display);
        return super.onCast();
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, PDamageType type) {
        return damage * damageReduction;
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        return damage * attackReduction;
    }

    @Override
    protected void cleanup() {
        if (display != null) {
            display.remove();
        }
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(damageReduction * 100, VALUE_COLOR));
        placeholderNames.add("damage reduction");
        spellAddedPlaceholders.add(Component.text(attackReduction * 100, VALUE_COLOR));
        placeholderNames.add("attack reduction");
        return super.getPlaceholders(c);
    }
}
