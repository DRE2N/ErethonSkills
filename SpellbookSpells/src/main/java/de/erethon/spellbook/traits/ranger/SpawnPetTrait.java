package de.erethon.spellbook.traits.ranger;

import de.erethon.papyrus.DamageType;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellTrait;
import de.erethon.spellbook.api.TraitData;
import de.erethon.spellbook.events.PetDeathEvent;
import de.erethon.spellbook.spells.ranger.pet.RangerPet;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SpawnPetTrait extends SpellTrait implements Listener {

    private final double attributeModifier = data.getDouble("attributeModifier", 0.8);
    private final long petRespawnDelay = data.getInt("petRespawnDelay", 6000);

    private RangerPet pet;
    private long deathTime = 0;

    public SpawnPetTrait(TraitData data, LivingEntity caster) {
        super(data, caster);
    }

    @Override
    protected void onAdd() {
        spawn();
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());
    }

    public void spawn() {
        pet = new RangerPet(caster, caster.getWorld(), EntityType.PIG);
        pet.teleport(caster.getLocation().getBlockX(), caster.getLocation().getBlockY(), caster.getLocation().getBlockZ());
        pet.setScaledAttributes(attributeModifier);
        pet.addToWorld();
        Spellbook.getInstance().getPetLookup().add(caster, pet);
    }

    @Override
    protected void onRemove() {
        pet.remove();
        Spellbook.getInstance().getPetLookup().remove(caster);
    }

    @Override
    public double onAttack(LivingEntity target, double damage, DamageType type) {
        if (pet.isShouldAttackAutomatically()) {
            pet.makeAttack(target);
        }
        return super.onAttack(target, damage, type);
    }

    @Override
    protected void onTick() {
        if (pet == null && System.currentTimeMillis() > deathTime + petRespawnDelay) {
            spawn();
        }
    }

    @EventHandler
    public void onPetDeath(PetDeathEvent event) {
        if (event.getPet() != pet) return;
        caster.sendParsedActionBar("<#ff0000>Dein Begleiter ist gestorben!");
        pet = null;
        deathTime = System.currentTimeMillis();
    }
}
