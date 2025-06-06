package de.erethon.spellbook.spells.ranger.pathfinder;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCastEvent;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.ranger.RangerBaseSpell;
import de.slikey.effectlib.effect.CircleEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;

public class EarthCurvature extends RangerBaseSpell implements Listener {

    private final double range = data.getDouble("range", 6);

    private CircleEffect effect;

    public EarthCurvature(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    public boolean onCast() {
        effect = new CircleEffect(Spellbook.getInstance().getEffectManager());
        effect.radius = (float) range;
        effect.setLocation(caster.getLocation().add(0, 0.2,0));
        effect.orient = false;
        effect.particle = Particle.DUST;
        effect.particleSize = 0.5f;
        effect.color = Color.YELLOW;
        effect.duration = 50 * keepAliveTicks;
        effect.start();
        spawnFallingBlocks(caster.getLocation());
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_TURTLE_EGG_CRACK, SoundCategory.RECORDS, 2, 0);
        for (LivingEntity living : caster.getLocation().getNearbyLivingEntities(range)) {
            living.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 1));
            living.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, living, true, Attribute.ADVANTAGE_MAGICAL), caster, PDamageType.MAGIC);
        }
        return super.onCast();
    }

    @EventHandler
    public void onSpellCast(SpellCastEvent event) {
        LivingEntity living = event.getCaster();
        if (!Spellbook.canAttack(caster, living)) return;
        if (living.getLocation().distance(caster.getLocation()) > range) return;
        living.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, living, true, Attribute.ADVANTAGE_MAGICAL), caster, PDamageType.MAGIC);
        living.getWorld().playSound(living.getLocation(), Sound.ENTITY_BLAZE_HURT, SoundCategory.RECORDS, 1, 1);
    }

    @Override
    protected void cleanup() {
        effect.cancel();
        HandlerList.unregisterAll(this);
        super.cleanup();
    }

    private void spawnFallingBlocks(Location origin) {
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            Location location = origin.clone().add(random.nextInt(-2, 2), 1, random.nextInt(-2, 2));
            BlockData blockData = location.getWorld().getHighestBlockAt(location).getBlockData();
            FallingBlock block = origin.getWorld().spawnFallingBlock(location.getWorld().getHighestBlockAt(location).getLocation(), blockData);
            block.setDropItem(false);
            block.setHurtEntities(false);
            block.setVelocity(new Vector(random.nextDouble(-1, 1), 0.8, random.nextDouble(-1, 1)));
        }
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(range, VALUE_COLOR));
        placeholderNames.add("range");
        spellAddedPlaceholders.add(Component.text(Spellbook.getVariedAttributeBasedDamage(data, caster, caster, true, Attribute.ADVANTAGE_MAGICAL), ATTR_MAGIC_COLOR));
        placeholderNames.add("damage");
        return super.getPlaceholders(c);
    }
}
