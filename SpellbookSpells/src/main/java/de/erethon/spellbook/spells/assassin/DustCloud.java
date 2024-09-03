package de.erethon.spellbook.spells.assassin;

import de.erethon.papyrus.PDamageType;
import de.erethon.spellbook.api.SpellCaster;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.spells.AoEBaseSpell;
import de.erethon.spellbook.utils.AssassinUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionType;

import java.util.List;

public class DustCloud extends AoEBaseSpell {

    private int invisCooldown = data.getInt("invisCooldown", 20);


    public DustCloud(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
    }

    @Override
    public boolean onPrecast() {
        return super.onPrecast() && AssassinUtils.hasEnergy(caster, data);
    }

    @Override
    public boolean onCast() {
        keepAliveTicks = 200;
        AreaEffectCloud cloud = target.getWorld().spawn(target, AreaEffectCloud.class);
        cloud.setColor(Color.GRAY);
        cloud.setDuration(200);
        cloud.setRadius(2);
        cloud.setBasePotionType(PotionType.THICK);
        return super.onCast();
    }

    @Override
    protected void onAfterCast() {
        caster.removeEnergy(data.getInt("energyCost", 0));
    }

    @Override
    protected void onTick() {
        super.onTick();
        invisCooldown--;
        if (invisCooldown <= 0 && !caster.isInvisible() && getEntities().contains(caster)) {
            caster.setInvisible(true);
        }
    }

    @Override
    protected void onEnter(LivingEntity entity) {
        if (entity == caster && invisCooldown <= 0) {
            caster.setInvisible(true);
        }
    }

    @Override
    protected void onLeave(LivingEntity entity) {
        if (entity == caster) {
            caster.setInvisible(false);
        }
    }

    @Override
    public double onAttack(LivingEntity target, double damage, PDamageType type) {
        caster.setInvisible(false);
        invisCooldown = data.getInt("invisCooldown", 20);
        return super.onAttack(target, damage, type);
    }

    @Override
    protected void cleanup() {
        caster.setInvisible(false);
    }

    @Override
    protected void onTickFinish() {
        super.onTickFinish();
    }

    @Override
    public List<Component> getPlaceholders(SpellCaster c) {
        spellAddedPlaceholders.add(Component.text(invisCooldown, VALUE_COLOR));
        return super.getPlaceholders(caster);
    }
}
