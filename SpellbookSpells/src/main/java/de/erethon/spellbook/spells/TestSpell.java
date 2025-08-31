package de.erethon.spellbook.spells;

import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.aoe.AoE;
import de.erethon.spellbook.api.SpellData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Listener;


public class TestSpell extends SpellbookBaseSpell implements Listener {

    public TestSpell(LivingEntity caster, SpellData spellData) {
        super(caster, spellData);
        keepAliveTicks = 2000;
        Bukkit.getPluginManager().registerEvents(this, Spellbook.getInstance().getImplementer());

    }

    @Override
    public boolean onCast() {
        AoE circularAoE = createCircularAoE(caster.getLocation(), 7, 3, 200);

        AoE rectangularAoE = createRectangularAoE(caster.getLocation(), 5, 10, 3, caster.getLocation().getDirection(), 200);

        AoE coneAoE = createConeAoE(caster.getLocation(), 10, 90, 3, caster.getLocation().getDirection(), 200)
                .onEnter((aoe, entity) -> {
                    entity.setFireTicks(100);
                })
                .onLeave((aoe, entity) -> {
                    entity.sendMessage("You left the fire zone!");
                })
                .onTick(aoe -> {
                });
                coneAoE.addDisplay(coneAoE.createDisplay()
                        .blockDisplay(Material.FIRE)
                        .scale(2.0f)
                        .translate(0, 1, 0)
                        .build())
                .addDisplay(coneAoE.createDisplay()
                        .textDisplay("<red>Danger Zone!")
                        .scale(1.5f)
                        .translate(0, 3, 0)
                        .build())
                .addBlocksOnTopGroundLevel(Material.FIRE)
                .sendBlockChanges();

        return true;
    }



    @Override
    public LivingEntity getTarget() {
        return null;
    }

    @Override
    public void setTarget(LivingEntity target) {

    }
}
