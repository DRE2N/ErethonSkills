package de.erethon.spellbook.spells.ranger.hawkeye;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.ranger.RangerBaseSpell;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;

public class ArrowRain extends RangerBaseSpell implements Listener {

    // Shoots a hail of arrows in front of the caster, dealing damage and applying slowness to all entities hit

    private final int radius = data.getInt("radius", 5);
    private final int effectDuration = data.getInt("effectDuration", 100);
    private final int effectStacks = data.getInt("effectStacks", 1);

    private final EffectData slowness = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Slow");

    private Random random;

    public ArrowRain(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast();
    }

    @Override
    public boolean onCast() {
        random = new Random();
        tickInterval = 5;
        keepAliveTicks = 100;
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
        return true;
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (event.getEntity().getShooter() != caster || event.getHitEntity() == null || !(event.getHitEntity() instanceof LivingEntity hit)) return;
        if (!Spellbook.canAttack(caster, hit)) {
            return;
        }
        triggerTraits(hit);
        hit.addEffect(caster, slowness, effectDuration, effectStacks);
    }

    @Override
    protected void onTick() {
        Location castLocation = getOffsetLocation(15);
        Arrow arrow = castLocation.getWorld().spawn(castLocation, Arrow.class);
        arrow.setShooter(caster);
        arrow.setSilent(true);
        Vector direction = caster.getLocation().getDirection().clone().multiply(0.5).add(new Vector(0, 0.5, 0));
        arrow.setVelocity(direction);
    }

    private Location getOffsetLocation(int yOffset) {
        Location location = caster.getLocation();
        int xOffset = (random.nextBoolean() ? 1 : -1) * random.nextInt(radius + 1);
        int zOffset = (random.nextBoolean() ? 1 : -1) * random.nextInt(radius + 1);
        return location.clone().add(xOffset, yOffset, zOffset);
    }

    @Override
    public void onAfterCast() {
        caster.setCooldown(data);
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(radius, VALUE_COLOR));
        placeholderNames.add("radius");
        return super.getPlaceholders(c);
    }
}
