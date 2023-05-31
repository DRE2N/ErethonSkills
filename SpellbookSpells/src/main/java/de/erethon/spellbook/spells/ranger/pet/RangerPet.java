package de.erethon.spellbook.spells.ranger.pet;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.Wolf;
import org.bukkit.Color;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftLivingEntity;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class RangerPet extends Wolf {

    private final FollowOwnerGoal followOwnerGoal = new FollowOwnerGoal(this, 1.0D, 2.0F, 10.0F, false);
    private final Transformation petStatusTextTransformation = new Transformation(new Vector3f(0, 1.1f, 0), new AxisAngle4f(0, 0, 0, 0), new Vector3f(0.5f, 0.5f, 0.5f), new AxisAngle4f(0, 0, 0, 0));

    private EntityType<?> petDisplayType = EntityType.COW;
    private final LivingEntity owner;
    private final org.bukkit.entity.Mob bukkitMob = getBukkitMob();
    private TextDisplay statusDisplay;

    private boolean shouldAttackAutomatically = true;
    private boolean isCurrentlyGoingToLocation = false;

    public RangerPet(org.bukkit.entity.LivingEntity bukkitOwner, World world, org.bukkit.entity.EntityType craftDisplayType) {
        super(EntityType.WOLF, ((CraftWorld) world).getHandle());
        CraftLivingEntity craftOwner = (CraftLivingEntity) bukkitOwner;
        this.owner = craftOwner.getHandle();
        setPetDisplayType(craftDisplayType);
        setTame(true);
        setOwnerUUID(owner.getUUID());
        setCustomName(owner.getName());
        setSilent(true);
        persist = false;
        goalSelector.removeAllGoals(goal -> true);
        goalSelector.addGoal(9, followOwnerGoal);
        goalSelector.addGoal(0, new MeleeAttackGoal(this, 2.0D, true));
        statusDisplay = world.spawn(bukkitOwner.getLocation(), TextDisplay.class, display -> {
            display.setTransformation(petStatusTextTransformation);
            display.text(Component.empty());
            display.setAlignment(TextDisplay.TextAligment.CENTER);
            display.setBillboard(Display.Billboard.VERTICAL);
            display.setBackgroundColor(Color.fromARGB(0, 1, 1, 1));
        });
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(getId(), getUUID(), getX(), getY(), getZ(), getYRot(), getXRot(), petDisplayType, 0, getDeltaMovement(), getYHeadRot(), this);
    }

    public void setScaledAttributes(double attributeMultiplier) {
        for (Attribute attribute : BuiltInRegistries.ATTRIBUTE.stream().toList()) {
            if (attribute == Attributes.MOVEMENT_SPEED) continue; // We don't want to make the pet extremely slow
            if (attribute == Attributes.ATTACK_SPEED) continue; // We don't want to make the pet attack extremely slow either
            if (getAttribute(attribute) == null || owner.getAttribute(attribute) == null) continue;
            getAttribute(attribute).setBaseValue(owner.getAttribute(attribute).getBaseValue() * attributeMultiplier);
        }
        setHealth(getMaxHealth());
    }

    public void makeAttack(org.bukkit.entity.LivingEntity bukkitLiving) {
        CraftLivingEntity craftLiving = (CraftLivingEntity) bukkitLiving;
        LivingEntity living = craftLiving.getHandle();
        setTarget(living, EntityTargetEvent.TargetReason.CUSTOM, false);
        statusDisplay.text(Component.text("ATTACKING").color(NamedTextColor.RED));
    }

    public void goToLocation(int x, int y, int z) {
        goalSelector.removeGoal(followOwnerGoal);
        moveControl.setWantedPosition(x, y, z, 1.0D);
    }

    public void setPetDisplayType(org.bukkit.entity.EntityType type) {
        this.petDisplayType = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(type.getKey().toString()));
        displayEntityType = petDisplayType;
    }

    public void teleport(int x, int y, int z) {
        teleportTo(x, y, z);
    }

    public void addToWorld() {
        level.addFreshEntity(this);
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

    public void setShouldAttackAutomatically(boolean shouldAttackAutomatically) {
        this.shouldAttackAutomatically = shouldAttackAutomatically;
    }

    public void setCurrentlyGoingToLocation(boolean currentlyGoingToLocation) {
        isCurrentlyGoingToLocation = currentlyGoingToLocation;
    }

    // Overrides
    @Override
    public void die(DamageSource damageSource) {
        statusDisplay.remove();
        super.die(damageSource);
    }

    @Override
    public void remove(RemovalReason reason) {
        statusDisplay.remove();
        super.remove(reason);
    }

}
