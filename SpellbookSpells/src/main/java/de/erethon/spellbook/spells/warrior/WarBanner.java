package de.erethon.spellbook.spells.warrior;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.slikey.effectlib.effect.CircleEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;

public class WarBanner extends SpellbookSpell {

    private ItemStack itemStack;
    private ItemDisplay banner;
    private int bannerHealth = 20;
    protected Wolf bannerHolder;
    protected final int radius = data.getInt("radius", 5);

    public WarBanner(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        bannerHealth = spellData.getInt("bannerHealth", 20);
        itemStack = new ItemStack(Material.RED_BANNER);
    }

    protected void spawnBanner(Location location) {
        bannerHolder = location.getWorld().spawn(location, Wolf.class, wolf -> {
            wolf.setCustomName("War Banner");
            wolf.setInvisible(true);
            wolf.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bannerHealth);
            wolf.setSilent(true);
            wolf.setOwner((AnimalTamer) caster);
            banner = location.getWorld().spawn(location, ItemDisplay.class, itemDisplay -> {
                itemDisplay.setItemStack(itemStack);
                //itemDisplay.setTransformation(TransformationUtil.scale(itemDisplay.getTransformation(), 2));
            });
            wolf.addPassenger(banner);
        });
    }

    @Override
    protected void onTick() {
        CircleEffect effect = new CircleEffect(Spellbook.getInstance().getEffectManager());
        effect.setEntity(bannerHolder);
        effect.duration = 50 * 10;
        effect.radius = radius;
        effect.wholeCircle = true;
        effect.start();
    }

    @Override
    protected void cleanup() {
        banner.remove();
        bannerHolder.remove();
    }
}
