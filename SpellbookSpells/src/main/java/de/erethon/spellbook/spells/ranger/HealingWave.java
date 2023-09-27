package de.erethon.spellbook.spells.ranger;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.slikey.effectlib.effect.WaveEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class HealingWave extends RangerBaseSpell {

    private final Set<FallingBlock> blocks = new HashSet<>();

    public HealingWave(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 500;
    }

    @Override
    public boolean onCast() {
        Location base = caster.getLocation().add(caster.getLocation().getDirection().multiply(2));
        Vector direction = caster.getLocation().getDirection();
        for (int i = 0; i < 10; i++) {
            Location location = base.clone().add(direction.getX() + i, direction.getY(), direction.getZ());
            FallingBlock block = base.getWorld().spawnFallingBlock(location, Material.BLUE_CONCRETE.createBlockData());
            block.setCancelDrop(true);
            block.setHurtEntities(false);
            block.setVelocity(caster.getLocation().getDirection().multiply(1.5f));
            blocks.add(block);
        }
        return super.onCast();
    }

    @Override
    protected void cleanup() {
        super.cleanup();
    }
}
