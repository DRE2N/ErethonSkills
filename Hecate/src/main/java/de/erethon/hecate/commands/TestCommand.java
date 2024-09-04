package de.erethon.hecate.commands;

import de.erethon.bedrock.command.ECommand;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

public class TestCommand extends ECommand {

    GlobalTranslator translator;
    TranslationRegistry reg;

    public TestCommand() {
        setCommand("test");
        setAliases("s");
        setMinArgs(0);
        setMaxArgs(3);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setHelp("Help.");
        setPermission("hecate.reload");

        translator = GlobalTranslator.translator();
        reg = TranslationRegistry.create(Key.key("hecate"));
        reg.register("test", Locale.GERMAN, new MessageFormat("testDE"));
        reg.register("test", Locale.ENGLISH, new MessageFormat("testEN"));
        translator.addSource(reg);
    }


    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        SpellData data = Bukkit.getServer().getSpellbookAPI().getLibrary().getSpellByID(args[1]);
        if (data == null) {
            commandSender.sendMessage("Spell not found.");
            return;
        }
        Player player = (Player) commandSender;
        SpellbookSpell spell = data.getActiveSpell(player);
        player.sendMessage(Component.translatable("test"));
        player.sendMessage(Component.translatable("spellbook.spell.name." + data.getId(), data.getName()));
        for (int i = 0; i < data.getDescriptionLineCount(); i++) {
            player.sendMessage(Component.translatable("spellbook.spell.description." + data.getId() + "." + i, spell.getPlaceholders(player)));
        }

    }





}

