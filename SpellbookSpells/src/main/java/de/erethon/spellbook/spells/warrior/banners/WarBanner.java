package de.erethon.spellbook.spells.warrior.banners;

import com.destroystokyo.paper.ParticleBuilder;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.spells.warrior.WarriorBaseSpell;
import de.erethon.spellbook.utils.TransformationUtil;
import de.slikey.effectlib.effect.CircleEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class WarBanner extends WarriorBaseSpell {
    protected ItemStack itemStack;
    private ItemDisplay banner;
    protected Wolf bannerHolder;
    public int radius = data.getInt("radius", 5);
    private final int bannerHealth = data.getInt("bannerHealth", 20);
    private final Particle particle = new ParticleBuilder(Particle.DUST).color(Spellbook.parseColor(data.getString("ringColor", "16777215"))).particle();

    public WarBanner(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
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
        effect.particle = particle;
        effect.duration = 50 * 10;
        effect.radius = radius;
        effect.enableRotation = false;
        effect.orient = false;
        effect.updateDirections = false;
        effect.wholeCircle = true;
        effect.start();
    }

    @Override
    protected void cleanup() {
        banner.remove();
        bannerHolder.remove();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(radius, VALUE_COLOR));
        placeholderNames.add("radius");
        return super.getPlaceholders(c);
    }
}
