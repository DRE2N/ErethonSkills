package de.erethon.hecate.commands;

import de.erethon.bedrock.command.ECommand;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookSpell;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

public class DisplayCommand extends ECommand {

    GlobalTranslator translator;
    TranslationRegistry reg;

    public DisplayCommand() {
        setCommand("display");
        setAliases("d");
        setMinArgs(0);
        setMaxArgs(3);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setHelp("Help.");
        setPermission("hecate.display");

        translator = GlobalTranslator.translator();
        reg = TranslationRegistry.create(Key.key("hecate"));
        reg.register("test", Locale.GERMAN, new MessageFormat("testDE"));
        reg.register("test", Locale.ENGLISH, new MessageFormat("testEN"));
        translator.addSource(reg);
    }


    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        if (args.length < 2) {
            commandSender.sendMessage("Usage: /h display <spell>");
            return;
        }
        SpellData data = Bukkit.getServer().getSpellbookAPI().getLibrary().getSpellByID(args[1]);
        if (data == null) {
            commandSender.sendMessage("Spell not found.");
            return;
        }
        Player player = (Player) commandSender;
        SpellbookSpell spell = data.getActiveSpell(player);
        player.sendMessage(Component.translatable("spellbook.spell.name." + data.getId(), data.getName()));
        spell.getPlaceholders(player);
        for (int i = 0; i < data.getDescriptionLineCount(); i++) {
            player.sendMessage(Component.translatable("spellbook.spell.description." + data.getId() + "." + i, spell.getPlaceholders(player)));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Bukkit.getServer().getSpellbookAPI().getLibrary().getLoaded().keySet().stream().toList();
        }
        return super.onTabComplete(sender, args);
    }
}

