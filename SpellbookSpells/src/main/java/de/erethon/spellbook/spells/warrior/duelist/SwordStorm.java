package de.erethon.spellbook.spells.warrior.duelist;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class SwordStorm extends DuelistBaseSpell {

    private double radius = data.getDouble("startRadius", 2);
    private double effectiveRadiusPerTick;
    private final double radiusPerTick = data.getDouble("radiusPerTick", 0.2);
    private final NamespacedKey key = new NamespacedKey("spellbook", "swordstorm_duelist");
    private final AttributeModifier attackBaseMod = new AttributeModifier(key, data.getDouble("attackModifier", -5), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

    private static final int SWORD_COUNT = 4;
    private final List<ItemDisplay> orbitingSwords = new ArrayList<>();
    private boolean parryEmpowered = false;

    public SwordStorm(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    public boolean onCast() {
        effectiveRadiusPerTick = radiusPerTick;

        if (caster.getTags().contains(PARRY_TAG)) {
            caster.getTags().remove(PARRY_TAG);
            parryEmpowered = true;
            effectiveRadiusPerTick *= 1.2;
        }

        createOrbitingSwords();
        playInitialEffect();
        caster.getAttribute(Attribute.ATTACK_DAMAGE).addTransientModifier(attackBaseMod);
        return super.onCast();
    }

    private void createOrbitingSwords() {
        Location center = caster.getLocation().add(0, 1.2, 0);
        Color swordColor = parryEmpowered ? DUELIST_PRIMARY : DUELIST_ACCENT;

        for (int i = 0; i < SWORD_COUNT; i++) {
            final int index = i;
            ItemDisplay sword = center.getWorld().spawn(center, ItemDisplay.class, d -> {
                d.setItemStack(new ItemStack(Material.IRON_SWORD));
                d.setBillboard(Display.Billboard.FIXED);
                d.setPersistent(false);
                d.setGlowing(true);
                d.setGlowColorOverride(swordColor);
                d.setInterpolationDuration(2);
                updateSwordTransform(d, index, 0, radius);
            });
            orbitingSwords.add(sword);
        }
    }

    private void updateSwordTransform(ItemDisplay sword, int index, float time, double currentRadius) {
        double angle = (Math.PI * 2.0 * index / SWORD_COUNT) + time;
        float heightOffset = (float) (Math.sin(time * 2 + index * 0.8) * 0.25f);
        float x = (float) (Math.cos(angle) * currentRadius * 0.85);
        float z = (float) (Math.sin(angle) * currentRadius * 0.85);

        Quaternionf rotation = new Quaternionf();
        rotation.rotateY((float) angle + (float) Math.PI / 2);
        rotation.rotateX((float) Math.PI / 6);
        rotation.rotateZ(time * 2.5f);

        sword.setTransformation(new Transformation(
            new Vector3f(x, heightOffset, z),
            rotation,
            new Vector3f(1.1f, 1.1f, 1.1f),
            new Quaternionf()
        ));
    }

    @Override
    protected void onTick() {
        super.onTick();

        Location casterLoc = caster.getLocation();
        Location center = new Location(casterLoc.getWorld(), casterLoc.getX(), casterLoc.getY() + 1.2, casterLoc.getZ());
        float time = currentTicks * 0.5f;

        for (int i = 0; i < orbitingSwords.size(); i++) {
            ItemDisplay sword = orbitingSwords.get(i);
            if (sword == null || !sword.isValid()) continue;
            sword.teleport(center);
            sword.setInterpolationDelay(-1);
            updateSwordTransform(sword, i, time, radius);

            if (currentTicks % 2 == 0) {
                Location swordLoc = center.clone().add(
                    Math.cos((Math.PI * 2.0 * i / SWORD_COUNT) + time) * radius * 0.85,
                    (Math.sin(time * 2 + i * 0.8) * 0.25),
                    Math.sin((Math.PI * 2.0 * i / SWORD_COUNT) + time) * radius * 0.85
                );
                Color dustColor = parryEmpowered ? DUELIST_PRIMARY : DUELIST_ACCENT;
                casterLoc.getWorld().spawnParticle(Particle.DUST, swordLoc, 1, 0.05, 0.05, 0.05, 0,
                    new Particle.DustOptions(dustColor, 0.8f));
            }
        }

        radius += effectiveRadiusPerTick;

        caster.swingMainHand();
        for (LivingEntity living : casterLoc.getNearbyLivingEntities(radius)) {
            if (living == caster || !Spellbook.canAttack(caster, living)) continue;
            living.setNoDamageTicks(0);
            double damageBonus = Spellbook.getScaledValue(data, caster, living, Attribute.ADVANTAGE_PHYSICAL);
            if (parryEmpowered) damageBonus *= 1.2;
            AttributeModifier attackBonus = new AttributeModifier(key, damageBonus, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
            caster.getAttribute(Attribute.ATTACK_DAMAGE).addTransientModifier(attackBonus);
            caster.attack(living);
            caster.getAttribute(Attribute.ATTACK_DAMAGE).removeModifier(attackBonus);
            living.getWorld().spawnParticle(Particle.SWEEP_ATTACK, living.getLocation().add(0, 1, 0), 1, 0.3, 0.3, 0.3, 0);
        }
    }

    private void playInitialEffect() {
        Location loc = caster.getLocation().add(0, 1, 0);
        Color color = parryEmpowered ? DUELIST_PRIMARY : DUELIST_ACCENT;
        for (int ring = 0; ring < 2; ring++) {
            double r = (ring + 1) * (radius / 2);
            int particles = (int) (r * 6);
            for (int i = 0; i < particles; i++) {
                double angle = (Math.PI * 2 * i / particles);
                Location p = loc.clone().add(Math.cos(angle) * r, 0, Math.sin(angle) * r);
                caster.getWorld().spawnParticle(Particle.DUST, p, 1, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(color, 1.0f));
            }
        }
        caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 4, radius * 0.4, 0.3, radius * 0.4, 0);
        caster.getWorld().playSound(casterLoc(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
        caster.getWorld().playSound(casterLoc(), Sound.ITEM_TRIDENT_RIPTIDE_1, 0.5f, parryEmpowered ? 1.8f : 1.5f);
    }

    private Location casterLoc() {
        return caster.getLocation();
    }

    @Override
    protected void cleanup() {
        caster.getAttribute(Attribute.ATTACK_DAMAGE).removeModifier(attackBaseMod);
        for (ItemDisplay sword : orbitingSwords) {
            if (sword == null || !sword.isValid()) continue;
            Location loc = sword.getLocation();
            loc.getWorld().spawnParticle(Particle.DUST, loc, 6, 0.2, 0.2, 0.2, 0,
                new Particle.DustOptions(parryEmpowered ? DUELIST_PRIMARY : DUELIST_ACCENT, 1.0f));
            loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 1);
            sword.remove();
        }
        orbitingSwords.clear();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(radius, VALUE_COLOR));
        placeholderNames.add("radius");
        spellAddedPlaceholders.add(Component.text(radiusPerTick, VALUE_COLOR));
        placeholderNames.add("radius per tick");
        spellAddedPlaceholders.add(Component.text(Spellbook.getScaledValue(data, caster, caster, Attribute.ADVANTAGE_PHYSICAL), ATTR_PHYSICAL_COLOR));
        placeholderNames.add("damage bonus");
        return super.getPlaceholders(c);
    }
}
