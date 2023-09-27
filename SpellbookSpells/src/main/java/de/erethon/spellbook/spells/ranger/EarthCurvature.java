package de.erethon.spellbook.spells.ranger;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCastEvent;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.CircleEffect;
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

import java.util.Random;

public class EarthCurvature extends RangerBaseSpell implements Listener {

    private final double range = data.getDouble("range", 6);

    private CircleEffect effect;

    public EarthCurvature(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = spellData.getInt("duration", 1000);
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    public boolean onCast() {
        effect = new CircleEffect(Spellbook.getInstance().getEffectManager());
        effect.radius = (float) range;
        effect.setLocation(caster.getLocation().add(0, 0.2,0));
        effect.orient = false;
        effect.particle = Particle.REDSTONE;
        effect.particleSize = 0.5f;
        effect.color = Color.YELLOW;
        effect.duration = 50 * keepAliveTicks;
        effect.start();
        spawnFallingBlocks(caster.getLocation());
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_TURTLE_EGG_CRACK, SoundCategory.RECORDS, 2, 0);
        for (LivingEntity living : caster.getLocation().getNearbyLivingEntities(range)) {
            living.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 100, 1));
            living.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, living, true, Attribute.ADV_PHYSICAL), caster, DamageType.MAGIC);
        }
        return super.onCast();
    }

    @EventHandler
    public void onSpellCast(SpellCastEvent event) {
        LivingEntity living = event.getCaster();
        if (!Spellbook.canAttack(caster, living)) return;
        if (living.getLocation().distance(caster.getLocation()) > range) return;
        living.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, living, true, Attribute.ADV_MAGIC), caster, DamageType.MAGIC);
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
}
