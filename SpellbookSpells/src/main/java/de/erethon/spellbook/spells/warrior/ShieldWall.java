package de.erethon.spellbook.spells.warrior;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

public class ShieldWall extends SpellbookSpell {

    private int duration = data.getInt("duration", 600);
    private int shields = data.getInt("shields", 8);
    private double spacing = data.getDouble("spacing", 0.2);

    private Set<ItemDisplay> displays = new HashSet<>();
    private Transformation transformation = new Transformation(new Vector3f(0, 0, 0), new AxisAngle4f(0, 0, 0, 0), new Vector3f(1, 1, 1), new AxisAngle4f(0, 0, 1, 0));


    public ShieldWall(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration;
    }

    @Override
    protected boolean onCast() {
        caster.getUsedSpells().put(data, System.currentTimeMillis());
        int currentSpacing = 0;
        Vector position = caster.getLocation().toVector();
      Vector lookingDirection = caster.getTargetBlockExact(64).getLocation().toVector().subtract(position);
        Vector projection = new Vector(lookingDirection.getX(), 0, lookingDirection.getZ());
        Vector direction = lookingDirection.crossProduct(projection).normalize();
        for (int i = shields / 2; i > 0; i--) {
            Location loc = direction.add(new Vector(currentSpacing, 0, 0)).toLocation(caster.getWorld()).add(caster.getLocation());
            ItemDisplay dis = caster.getWorld().spawn(loc, ItemDisplay.class, itemDisplay -> {
                itemDisplay.setItemStack(new ItemStack(Material.SHIELD));
                itemDisplay.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIRSTPERSON_RIGHTHAND);
                itemDisplay.setTransformation(transformation);
            });
            MessageUtil.broadcastMessage("Spawned shield at " + dis.getLocation().toString());
            displays.add(dis);
            currentSpacing += spacing;
        }
        currentSpacing = 0;
        for (int i = shields / 2; i > 0; i--) {
            ItemDisplay dis = caster.getWorld().spawn(caster.getLocation().getDirection().add(new Vector(currentSpacing * -1, 0, 0)).toLocation(caster.getWorld()), ItemDisplay.class, itemDisplay -> {
                itemDisplay.setItemStack(new ItemStack(Material.SHIELD));
                itemDisplay.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIRSTPERSON_RIGHTHAND);
                itemDisplay.setTransformation(transformation);
            });
            displays.add(dis);
            currentSpacing += spacing;
        }
        return super.onCast();
    }

    @Override
    protected void cleanup() {
        for (ItemDisplay display : displays) {
            display.remove();
        }
    }
}
