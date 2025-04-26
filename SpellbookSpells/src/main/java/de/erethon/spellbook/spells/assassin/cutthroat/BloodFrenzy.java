package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

public class BloodFrenzy extends AssassinBaseSpell {

    // For some time, the assassin gains increased attack speed and movement speed, but takes increased damage from all sources and bleeds.
    // Additionally, the assassin is unable to use any other abilities while in this state.

    private final double attackSpeedBonus = data.getDouble("attackSpeedBonus", 0.2);
    private final double movementSpeedBonus = data.getDouble("movementSpeedBonus", 0.1);
    private final double defenseMalusPercent = data.getDouble("defenseMalusPercent", -0.2);
    private final int effectDuration = data.getInt("effectDuration", 10);
    private final int effectStacksPerSecond = data.getInt("effectStacksPerSecond", 1);
    private final AttributeModifier attackSpeedModifier = new AttributeModifier(NamespacedKey.fromString("spellbook:blood_frenzy_attack_speed"), attackSpeedBonus, AttributeModifier.Operation.ADD_NUMBER);
    private final AttributeModifier movementSpeedModifier = new AttributeModifier(NamespacedKey.fromString("spellbook:blood_frenzy_movement_speed"), movementSpeedBonus, AttributeModifier.Operation.ADD_NUMBER);
    private final AttributeModifier defenseMalus = new AttributeModifier(NamespacedKey.fromString("spellbook:blood_frenzy_defense_malus"), defenseMalusPercent, AttributeModifier.Operation.ADD_SCALAR);

    private EffectData bleeding = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Bleeding");
    private int ticks = 0;

    public BloodFrenzy(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration * 20;
    }

    @Override
    protected boolean onPrecast() {
        return AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    public boolean onCast() {
        caster.getAttribute(Attribute.ATTACK_SPEED).addTransientModifier(attackSpeedModifier);
        caster.getAttribute(Attribute.MOVEMENT_SPEED).addTransientModifier(movementSpeedModifier);
        caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).addTransientModifier(defenseMalus);
        caster.getAttribute(Attribute.RESISTANCE_MAGICAL).addTransientModifier(defenseMalus);
        caster.getAttribute(Attribute.RESISTANCE_AIR).addTransientModifier(defenseMalus);
        caster.getAttribute(Attribute.RESISTANCE_FIRE).addTransientModifier(defenseMalus);
        caster.getAttribute(Attribute.RESISTANCE_WATER).addTransientModifier(defenseMalus);
        caster.getAttribute(Attribute.RESISTANCE_EARTH).addTransientModifier(defenseMalus);
        caster.getTags().add("assassin.blood_frenzy");
        return super.onCast();
    }

    @Override
    protected void onTick() {
        ticks++;
        if (ticks == 20) {
            ticks = 0;
            caster.addEffect(caster, bleeding, effectDuration, effectStacksPerSecond);
        }
        caster.getWorld().spawnParticle(Particle.DUST, caster.getLocation(), 3, 0.5, 0.5, 0.5);
        super.onTick();
    }

    @Override
    public boolean onCast(SpellbookSpell spell) {
        caster.sendParsedActionBar("<red>You cannot use abilities during Blood Frenzy.</red>");
        return false;
    }

    @Override
    protected void onTickFinish() {
        caster.getAttribute(Attribute.ATTACK_SPEED).removeModifier(attackSpeedModifier);
        caster.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(movementSpeedModifier);
        caster.getAttribute(Attribute.RESISTANCE_PHYSICAL).removeModifier(defenseMalus);
        caster.getAttribute(Attribute.RESISTANCE_MAGICAL).removeModifier(defenseMalus);
        caster.getAttribute(Attribute.RESISTANCE_AIR).removeModifier(defenseMalus);
        caster.getAttribute(Attribute.RESISTANCE_FIRE).removeModifier(defenseMalus);
        caster.getAttribute(Attribute.RESISTANCE_WATER).removeModifier(defenseMalus);
        caster.getAttribute(Attribute.RESISTANCE_EARTH).removeModifier(defenseMalus);
        caster.getTags().remove("assassin.blood_frenzy");
        super.onTickFinish();
    }
}
