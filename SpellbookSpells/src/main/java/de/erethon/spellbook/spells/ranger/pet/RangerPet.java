package de.erethon.spellbook.spells.ranger.pet;

import de.erethon.bedrock.chat.MessageUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftLivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class RangerPet extends Wolf {

    private EntityType<?> petDisplayType = EntityType.COW;
    private final LivingEntity owner;

    public RangerPet(org.bukkit.entity.LivingEntity bukkitOwner, World world, org.bukkit.entity.EntityType craftDisplayType) {
        super(EntityType.WOLF, ((CraftWorld) world).getHandle());
        CraftLivingEntity craftOwner = (CraftLivingEntity) bukkitOwner;
        this.owner = craftOwner.getHandle();
        setPetDisplayType(craftDisplayType);
        setTame(true);
        setOwnerUUID(owner.getUUID());
        goalSelector.removeAllGoals(Predicate.not((x -> false)));
        goalSelector.addGoal(0, new FollowOwnerGoal(this, 1.0D, 2.0F, 10.0F, false));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 2.0D, true));
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(getId(), getUUID(), getX(), getY(), getZ(), getYRot(), getXRot(), petDisplayType, 0, getDeltaMovement(), getYHeadRot(), this);
    }

    public void setScaledAttributes(double attributeMultiplier) {
        for (Attribute attribute : BuiltInRegistries.ATTRIBUTE.stream().toList()) {
            if (attribute == Attributes.MOVEMENT_SPEED) continue; // We don't want to make the pet extremely slow
            if (getAttribute(attribute) == null || owner.getAttribute(attribute) == null) continue;
            getAttribute(attribute).setBaseValue(owner.getAttribute(attribute).getBaseValue() * attributeMultiplier);
        }
        setHealth(getMaxHealth());
    }

    public void makeAttack(org.bukkit.entity.LivingEntity bukkitLiving) {
        CraftLivingEntity craftLiving = (CraftLivingEntity) bukkitLiving;
        LivingEntity living = craftLiving.getHandle();
        setTarget(living);
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
    }
}
