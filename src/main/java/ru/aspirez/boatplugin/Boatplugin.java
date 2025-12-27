package ru.aspirez.boatplugin;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BoatPlugin extends JavaPlugin implements Listener {

    private static final String PERMISSION_RELOAD = "boatrestrictor.reload";
    private static final String CONFIG_PLAY_ERROR_SOUND = "play-error-sound";
    private static final String CONFIG_ERROR_SOUND = "error-sound";
    private static final String DEFAULT_LANGUAGE = "ru";
    private static final Sound DEFAULT_ERROR_SOUND = Sound.BLOCK_ANVIL_LAND;

    private final Set<String> restrictedWorlds = new HashSet<>();
    private final Set<String> restrictedBoats = new HashSet<>();
    private FileConfiguration languageConfig;
    private Sound errorSound;
    private boolean playErrorSound;

    @Override
    public void onEnable() {
        setupConfigs();
        registerEvents();
        setupCommand();
        logPluginInfo();
    }

    private void setupConfigs() {
        saveDefaultConfig();
        loadPluginConfig();
        loadLanguageConfig();
    }

    private void loadPluginConfig() {
        FileConfiguration config = getConfig();

        // Загрузка списков
        restrictedWorlds.clear();
        restrictedWorlds.addAll(config.getStringList("restricted-worlds"));

        restrictedBoats.clear();
        config.getStringList("restricted-boats").stream()
                .map(String::toLowerCase)
                .forEach(restrictedBoats::add);

        // Загрузка настроек звука
        playErrorSound = config.getBoolean(CONFIG_PLAY_ERROR_SOUND, true);
        errorSound = parseSound(config.getString(CONFIG_ERROR_SOUND, DEFAULT_ERROR_SOUND.name()));
    }

    private void loadLanguageConfig() {
        String language = getConfig().getString("lang", DEFAULT_LANGUAGE).toLowerCase(Locale.ENGLISH);
        File languageDir = new File(getDataFolder(), "lang");

        if (!languageDir.exists() && !languageDir.mkdirs()) {
            getLogger().warning("Не удалось создать директорию для языковых файлов!");
            return;
        }

        File languageFile = new File(languageDir, language + ".yml");

        // Если файл языка не существует, создаем из ресурсов
        if (!languageFile.exists()) {
            String resourcePath = "lang/" + language + ".yml";
            if (getResource(resourcePath) != null) {
                saveResource(resourcePath, false);
            } else {
                // Используем язык по умолчанию
                saveResource("lang/" + DEFAULT_LANGUAGE + ".yml", false);
                languageFile = new File(languageDir, DEFAULT_LANGUAGE + ".yml");
                getLogger().warning("Языковой файл '" + language + ".yml' не найден. Используется язык по умолчанию.");
            }
        }

        languageConfig = YamlConfiguration.loadConfiguration(languageFile);
    }

    private Sound parseSound(String soundName) {
        try {
            return Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            getLogger().warning("Звук '" + soundName + "' не найден. Используется звук по умолчанию.");
            return DEFAULT_ERROR_SOUND;
        }
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void setupCommand() {
        getCommand("boatrestrictor").setExecutor((sender, command, label, args) -> {
            if (sender.hasPermission(PERMISSION_RELOAD)) {
                reloadPlugin();
                sender.sendMessage(getLocalizedMessage("reload-message"));
            } else {
                sender.sendMessage(getLocalizedMessage("no-permission"));
            }
            return true;
        });
    }

    private void reloadPlugin() {
        reloadConfig();
        loadPluginConfig();
        loadLanguageConfig();
        getLogger().info("Конфигурация перезагружена.");
    }

    private void logPluginInfo() {
        Logger logger = getLogger();
        logger.info("Плагин BoatRestrictor успешно запущен!");
        logger.info("Ограниченных миров: " + restrictedWorlds.size());
        logger.info("Ограниченных лодок: " + restrictedBoats.size());
        logger.info("Воспроизведение звука ошибки: " + (playErrorSound ? "включено" : "выключено"));
    }

    @EventHandler
    public void onBoatPlace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !isRestrictedBoat(item)) {
            return;
        }

        Player player = event.getPlayer();
        if (!isWorldRestricted(player.getWorld().getName())) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() == Material.WATER) {
            return;
        }

        event.setCancelled(true);
        handleRestrictedPlacement(player);
    }

    private boolean isRestrictedBoat(ItemStack item) {
        String itemType = item.getType().name().toLowerCase();
        return restrictedBoats.contains(itemType);
    }

    private boolean isWorldRestricted(String worldName) {
        return restrictedWorlds.contains(worldName);
    }

    private void handleRestrictedPlacement(Player player) {
        player.sendMessage(getLocalizedMessage("deny-message"));

        if (playErrorSound && errorSound != null) {
            player.playSound(player.getLocation(), errorSound, 1.0f, 1.0f);
        }
    }

    private String getLocalizedMessage(String key) {
        if (languageConfig == null) {
            return "§c[Ошибка загрузки языка]";
        }

        String message = languageConfig.getString(key);
        return message != null ? message : "§c[Сообщение не найдено: " + key + "]";
    }

    // Геттеры для тестирования
    Set<String> getRestrictedWorlds() {
        return Collections.unmodifiableSet(restrictedWorlds);
    }

    Set<String> getRestrictedBoats() {
        return Collections.unmodifiableSet(restrictedBoats);
    }

    Sound getErrorSound() {
        return errorSound;
    }

    boolean isPlayErrorSound() {
        return playErrorSound;
    }
}