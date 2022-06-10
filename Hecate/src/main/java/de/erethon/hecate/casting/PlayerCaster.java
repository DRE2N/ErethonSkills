package de.erethon.hecate.casting;

import com.mojang.authlib.GameProfile;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.caster.SpellCaster;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.bukkit.entity.LivingEntity;

public class PlayerCaster extends SpellCaster {

    public PlayerCaster(Spellbook spellbook, LivingEntity entity) {
        super(spellbook, entity);
    }
}
