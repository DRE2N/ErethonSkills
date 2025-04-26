package de.erethon.spellbook.spells.assassin.cutthroat;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.assassin.AssassinBaseSpell;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class ThroatSlash extends AssassinBaseSpell {

    // Ultimate ability: Jumps to a target, slashing their throat, dealing massive damage. If the target has less than 25% health, they instantly die.

    public ThroatSlash(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    protected boolean onPrecast() {
        return super.onPrecast() && lookForTarget();
    }

    @Override
    public boolean onCast() {
        if (target == null) {
            return false;
        }
        return super.onCast();
    }

    @Override
    protected void onTick() {
        if (target == null) {
            return;
        }
        double distance = target.getLocation().distance(caster.getLocation());
        // Have the caster jump to the target
        if (distance > 3) {
            caster.setVelocity(target.getLocation().subtract(caster.getLocation()).toVector().normalize().multiply(0.5));
        } else { // Now we are close enough, slashing time
            double healthPercent = target.getHealth() / target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            if (healthPercent < 0.25) {
                target.damage(Integer.MAX_VALUE, caster);
                for (int i = 0; i < 10; i++) {
                    target.getWorld().spawnParticle(Particle.DUST, target.getLocation(), 5, 2, 2, 2);
                }
                target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
                MessageUtil.sendMessage(target, "<dark_red>You were executed by " + caster.getScoreboardEntryName() + "!</dark_red>");
                triggerTraits(target, 1);
            } else {
                target.damage(Spellbook.getVariedAttributeBasedDamage(data, caster, target, true, Attribute.ADVANTAGE_PHYSICAL), caster);
                triggerTraits(target, 0);
            }
            currentTicks = keepAliveTicks;
        }
    }
}
