package de.erethon.hecate.translations;

import de.erethon.bedrock.config.MessageHandler;
import de.erethon.hecate.Hecate;

import java.io.File;

public class TranslationManager {

    private final Hecate plugin;
    private MessageHandler messageHandler;

    public TranslationManager(Hecate plugin) {
        this.plugin = plugin;
        reloadTranslations();
    }

    public void reloadTranslations() {
        File languagesDir = new File(plugin.getDataFolder(), "languages");
        if (!languagesDir.exists()) {
            boolean created = languagesDir.mkdirs();
            if (!created) {
                Hecate.log("Failed to create languages directory");
                return;
            }
        }

        // Save default language files if they don't exist
        plugin.saveResource("languages/english.yml", false);
        plugin.saveResource("languages/german.yml", false);

        this.messageHandler = new MessageHandler(languagesDir, "hecate");
        this.messageHandler.setDefaultLanguage("english");

        Hecate.log("Loaded translations from languages directory");
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }
}
