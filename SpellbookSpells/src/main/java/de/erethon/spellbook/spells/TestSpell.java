package de.erethon.spellbook.spells;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import de.erethon.spellbook.fx.Animation;
import de.erethon.spellbook.fx.cues.CircleBlockDisplayCue;
import de.erethon.spellbook.fx.cues.SweepingBlockDisplayCue;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Listener;


public class TestSpell extends SpellbookSpell implements Listener {

    public TestSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 2000;
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());

    }

    @Override
    protected boolean onPrecast() {
        return true;
    }

    @Override
    protected boolean onCast() {
        Animation animation = new Animation().addCue(0, new CircleBlockDisplayCue(Material.STONE, 5, 60, 5, 1.0f));
        animation.play(caster);
        return true;
    }

    @Override
    protected void onAfterCast() {
        caster.setCooldown(data);
    }

    @Override
    protected void onTick() {
    }
}
