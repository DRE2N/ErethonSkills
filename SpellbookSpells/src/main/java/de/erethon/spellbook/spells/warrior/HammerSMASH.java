package de.erethon.spellbook.spells.warrior;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import de.slikey.effectlib.effect.CircleEffect;
import de.slikey.effectlib.effect.CylinderEffect;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class HammerSMASH extends SpellbookSpell implements Listener {

    private final EffectManager manager = Spellbook.getInstance().getEffectManager();

    private int animationTicks = 0;
    private final Sound sound = Sound.sound(Key.key("entity.bat.takeoff"), Sound.Source.PLAYER, 0.5f, 0);
    private final Sound chargeSound = Sound.sound(Key.key("block.note_block.basedrum"), Sound.Source.PLAYER, 0.5f, 0);
    private final Sound impactSound = Sound.sound(Key.key("entity.ender_dragon.hurt"), Sound.Source.PLAYER, 0.5f, 0);
    private final double pushVector = data.getDouble("pushVector", 0.3);

    private final Set<FallingBlock> blocks = new HashSet<>();

    public HammerSMASH(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 120;
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    @Override
    protected boolean onCast() {
        return true;
    }

    @Override
    protected void onTick() {
        animationTicks++;
        if (animationTicks < 5) {
            caster.playSound(chargeSound);
        }
        if (animationTicks == 5) {
            caster.setVelocity(new Vector(0, pushVector, 0).add(caster.getLocation().getDirection()));
        }
        if (animationTicks >= 5 && animationTicks <= 10) {
            caster.playSound(sound);
        }
        if (animationTicks == 15) {
            caster.sendParsedActionBar("<red><bold>SMASH!");
            spawnFallingBlocks(caster.getLocation());
            CylinderEffect effect = new CylinderEffect(manager);
            effect.setLocation(caster.getLocation());
            effect.iterations = 100;
            effect.type = EffectType.REPEATING;
            effect.particle = Particle.REDSTONE;
            effect.particleSize = 1f;
            effect.color = Color.GRAY;
            effect.duration = 20 * 50;
            effect.height = 0.5f;
            effect.radius = 6.5f;
            effect.start();
            caster.playSound(impactSound);
            caster.getLocation().getNearbyEntitiesByType(LivingEntity.class, 6).forEach(entity -> {
                if (entity == caster) {
                    return;
                }
                entity.damage(Spellbook.getVariedAttributeBasedDamage(getData(), caster, entity, false, Attribute.ADV_PHYSICAL), caster, DamageType.PHYSICAL);
            });
          }
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
            blocks.add(block);
        }
    }

    @EventHandler
    private void onBlockLand(EntityChangeBlockEvent event) {
        if (blocks.contains(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @Override
    protected void onTickFinish() {
        blocks.forEach(FallingBlock::remove);
        HandlerList.unregisterAll(this);
    }
}
