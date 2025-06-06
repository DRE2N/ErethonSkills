package de.erethon.spellbook.spells.ranger.beastmaster.pet;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class RangerPet extends net.minecraft.world.entity.animal.wolf.Wolf {

    public static final NamespacedKey PET_STATUS_KEY = new NamespacedKey("spellbook", "pet_status");

    private final FollowOwnerGoal followOwnerGoal = new FollowOwnerGoal(this, 1.0D, 2.0F, 10.0F);
    private final RandomStrollGoal randomStrollGoal = new RandomStrollGoal(this, 1.0D, 500, false);
    private final MoveTowardsRestrictionGoal moveTowardsRestrictionGoal = new MoveTowardsRestrictionGoal(this, 1.0D);
    private final Transformation petStatusTextTransformation = new Transformation(
            new Vector3f(0, 1.1f, 0),
            new AxisAngle4f(0, 0, 0, 0),
            new Vector3f(0.5f, 0.5f, 0.5f),
            new AxisAngle4f(0, 0, 0, 0));
    private final LivingEntity owner;
    private final org.bukkit.entity.Mob bukkitMob;
    private TextDisplay statusDisplay;

    private boolean shouldAttackAutomatically = true;
    private boolean isCurrentlyGoingToLocation = false;
    private boolean isIdleMoving = false;

    public RangerPet(org.bukkit.entity.LivingEntity bukkitOwner, World world) {
        super(EntityType.WOLF, ((CraftWorld) world).getHandle());
        CraftLivingEntity craftOwner = (CraftLivingEntity) bukkitOwner;
        this.owner = craftOwner.getHandle();
        this.bukkitMob = (org.bukkit.entity.Mob) this.getBukkitEntity();
        setTame(true, false);
        setCustomName(owner.getName());
        setSilent(true);
        persist = false;
        goalSelector.removeAllGoals(goal -> true);
        goalSelector.addGoal(9, followOwnerGoal);
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5D, true));
        statusDisplay = world.spawn(bukkitOwner.getLocation(), TextDisplay.class, display -> {
            display.setTransformation(petStatusTextTransformation);
            display.text(Component.empty());
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setBillboard(Display.Billboard.VERTICAL);
            display.getPersistentDataContainer().set(PET_STATUS_KEY, PersistentDataType.BYTE, (byte) 1);
            display.setBackgroundColor(Color.fromARGB(0, 1, 1, 1));
            display.setPersistent(false);
        });
    }

    public void setScaledAttributes(double attributeMultiplier) {
        for (Attribute attribute : BuiltInRegistries.ATTRIBUTE.stream().toList()) {
            if (attribute == Attributes.MOVEMENT_SPEED) continue; // We don't want to make the pet extremely slow
            if (attribute == Attributes.ATTACK_SPEED) continue; // We don't want to make the pet attack extremely slow either
            if (attribute == Attributes.FOLLOW_RANGE) continue; // Don't change the follow range either
            if (attribute == Attributes.JUMP_STRENGTH) continue; // Don't change jumping
            if (attribute == Attributes.STEP_HEIGHT) continue; // Don't change stepping
            Holder<Attribute> holder = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute);
            if (getAttribute(holder) == null || owner.getAttribute(holder) == null) continue;
            getAttribute(holder).setBaseValue(owner.getAttribute(holder).getBaseValue() * attributeMultiplier);
        }
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        getAttribute(Attributes.STEP_HEIGHT).setBaseValue(1.0D);
        getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(0.41D);
        setHealth(getMaxHealth());
    }

    public void makeAttack(org.bukkit.entity.LivingEntity bukkitLiving) {
        CraftLivingEntity craftLiving = (CraftLivingEntity) bukkitLiving;
        LivingEntity living = craftLiving.getHandle();
        if (living == this || living == owner) return; // Don't attack yourself or your owner
        setTarget(living, EntityTargetEvent.TargetReason.CUSTOM);
        statusDisplay.text(Component.text("⚔").color(NamedTextColor.RED));
    }

    public void stopAttack() {
        stopBeingAngry();
        statusDisplay.text(Component.empty());
    }

    public void goToLocation(int x, int y, int z) {
        goalSelector.removeGoal(followOwnerGoal);
        navigation.moveTo(x, y, z, 1.0D);
        isCurrentlyGoingToLocation = true;
    }

    public void callback() {
        stopAttack();
        goalSelector.addGoal(9, followOwnerGoal);
        isCurrentlyGoingToLocation = false;
    }

    @Override
    public void tick() {
        super.tick();
        if (isCurrentlyGoingToLocation && navigation.isDone() || isCurrentlyGoingToLocation && navigation.isStuck()) { // If the pet is stuck or has reached the location
            isCurrentlyGoingToLocation = false;
        }
    }

    public void teleport(int x, int y, int z) {
        teleportTo(x, y, z);
    }

    public void addToWorld() {
        level().addFreshEntity(this);
        bukkitMob.addPassenger(statusDisplay);
    }

    public void remove() {
        statusDisplay.remove();
        remove(RemovalReason.DISCARDED);
    }

    public boolean isShouldAttackAutomatically() {
        return shouldAttackAutomatically;
    }

    public boolean isCurrentlyGoingToLocation() {
        return isCurrentlyGoingToLocation;
    }

    public Location getLocation() {
        return bukkitMob.getLocation();
    }

    public void setShouldAttackAutomatically(boolean shouldAttackAutomatically) {
        this.shouldAttackAutomatically = shouldAttackAutomatically;
    }

    public void setCurrentlyGoingToLocation(boolean currentlyGoingToLocation) {
        isCurrentlyGoingToLocation = currentlyGoingToLocation;
    }

    public org.bukkit.entity.LivingEntity getBukkitOwner() {
        return (org.bukkit.entity.LivingEntity) owner.getBukkitEntity();
    }

}

    // Overrides


    /*@Override
    public boolean doHurtTarget(Entity target) {
        bukkitMob.getWorld().spawnParticle(Particle.DUST, new Location(bukkitMob.getWorld(), target.getX(), target.getY() + 0.5, target.getZ()), 4,
                new Particle.DustOptions(Color.fromRGB(40, 180, 60), 1));
        PetAttackEvent event = new PetAttackEvent(this, (org.bukkit.entity.LivingEntity) target.getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        return super.doHurtTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount, CraftPDamageType type) {
        if (source.getEntity() == owner) {
            return false;
        }
        return super.hurt(source, amount, type);
    }

    @Override
    public void die(DamageSource damageSource) {
        statusDisplay.remove();
        Spellbook.getInstance().getPetLookup().remove(bukkitMob);
        PetDeathEvent event = new PetDeathEvent(this);
        Bukkit.getPluginManager().callEvent(event);
        super.die(damageSource);
    }

    @Override
    public void remove(RemovalReason reason) {
        statusDisplay.remove();
        Spellbook.getInstance().getPetLookup().remove(bukkitMob);
        super.remove(reason);
    }

}*/
