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
import java.util.List;
import java.util.Locale;

public class Boatplugin extends JavaPlugin implements Listener {

    private List<String> restrictedWorlds;
    private List<String> restrictedBoats;
    private FileConfiguration langConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        loadLanguage();

        getServer().getPluginManager().registerEvents(this, this);

        // Команда для перезагрузки
        getCommand("boatrestrictor").setExecutor((sender, command, label, args) -> {
            if (sender.hasPermission("boatrestrictor.reload")) {
                reloadConfig();
                loadConfig();
                loadLanguage();
                sender.sendMessage(lang("reload-message"));
            } else {
                sender.sendMessage(lang("no-permission"));
            }
            return true;
        });
    }

    private void loadConfig() {
        restrictedWorlds = getConfig().getStringList("restricted-worlds");
        restrictedBoats = getConfig().getStringList("restricted-boats");

        // Логируем загруженные настройки
        getLogger().info("Загружено ограниченных миров: " + restrictedWorlds.size());
        getLogger().info("Загружено ограниченных лодок: " + restrictedBoats.size());

        boolean playErrorSound = getConfig().getBoolean("play-error-sound", true);
        String soundName = getConfig().getString("error-sound", "BLOCK_ANVIL_LAND");

        getLogger().info("Воспроизведение звука ошибки: " + (playErrorSound ? "включено" : "выключено"));
        if (playErrorSound) {
            getLogger().info("Звук при ошибке: " + soundName);
        }
    }

    private void loadLanguage() {
        String lang = getConfig().getString("lang", "ru").toLowerCase(Locale.ENGLISH);
        File langDir = new File(getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        File langFile = new File(langDir, lang + ".yml");
        if (!langFile.exists()) {
            // Пытаемся скопировать ru.yml по умолчанию
            saveResource("lang/ru.yml", false);
            // Если нужный файл не найден — создаём его как копию ru
            if (!langFile.exists()) {
                getLogger().warning("Язык " + lang + " не найден. Создаю ru.yml по умолчанию.");
                saveResource("lang/" + lang + ".yml", true);
            }
        }

        // Загружаем файл языка
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    private String lang(String key) {
        if (langConfig == null) return "§c[Lang error]";
        return langConfig.getString(key, "§c[Missing lang: " + key + "]");
    }

    @EventHandler
    public void onBoatPlace(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = e.getPlayer();
        ItemStack item = e.getItem();
        if (item == null) return;

        String itemType = item.getType().name().toLowerCase();
        boolean isRestrictedBoat = restrictedBoats.stream()
                .anyMatch(boat -> boat.equalsIgnoreCase(itemType));
        if (!isRestrictedBoat) return;

        if (!restrictedWorlds.contains(player.getWorld().getName())) return;

        Block clickedBlock = e.getClickedBlock();
        if (clickedBlock == null) return;

        if (clickedBlock.getType() != Material.WATER) {
            e.setCancelled(true);
            player.sendMessage(lang("deny-message"));

            if (getConfig().getBoolean("play-error-sound", true)) {
                String soundName = getConfig().getString("error-sound", "BLOCK_ANVIL_LAND");
                try {
                    player.playSound(player.getLocation(), Sound.valueOf(soundName), 1.0f, 1.0f);
                } catch (IllegalArgumentException ex) {
                    getLogger().warning("Неверное имя звука в конфиге: " + soundName + " — звук не будет воспроизведён.");
                }
            }
        }
    }
}
