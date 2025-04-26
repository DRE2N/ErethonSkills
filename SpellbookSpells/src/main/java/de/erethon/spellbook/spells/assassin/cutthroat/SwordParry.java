package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.EffectData;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class SwordParry extends AssassinBaseSpell  {

    // RMB. For a short time, the assassin can parry incoming attacks, dealing damage to the attacker and stunning them for a short time.
    private final double damageMultiplier = data.getDouble("damageMultiplier", 1.0);
    private final int effectDuration = data.getInt("effectDuration", 10);

    private final EffectData stun = Bukkit.getServer().getSpellbookAPI().getLibrary().getEffectByID("Stun");

    public SwordParry(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = duration; // We want sub-second duration here
    }

    @Override
    protected boolean onPrecast() {
        return AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    public boolean onCast() {
        caster.getTags().add("assassin.parrying");
        return super.onCast();
    }

    @Override
    public double onDamage(LivingEntity attacker, double damage, PDamageType type) {
        if (attacker == caster) { // Self-attacks can't be parried
            return damage;
        }
        triggerTraits();
        attacker.damage(damage * damageMultiplier, caster, type);
        attacker.addEffect(caster, stun, effectDuration, 1);
        attacker.interrupt();
        caster.getTags().remove("assassin.parrying");
        return 0;
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
        caster.getTags().remove("assassin.parrying");
    }
}
