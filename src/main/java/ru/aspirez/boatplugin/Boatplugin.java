package ru.aspirez.boatplugin;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class Boatplugin extends JavaPlugin implements Listener {

    private List<String> restrictedWorlds;
    private List<String> restrictedBoats;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);

        // Команда для перезагрузки конфига
        getCommand("boatrestrictor").setExecutor((sender, command, label, args) -> {
            if (sender.hasPermission("boatrestrictor.reload")) {
                reloadConfig();
                loadConfig();
                sender.sendMessage("§aКонфигурация BoatRestrictor перезагружена!");
            } else {
                sender.sendMessage("§cУ вас нет прав на эту команду!");
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
    }

    @EventHandler
    public void onBoatPlace(PlayerInteractEvent e) {
        // Проверяем, что это правый клик по блоку
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = e.getPlayer();
        ItemStack item = e.getItem();

        // Проверяем, что игрок держит предмет
        if (item == null) return;

        // Проверяем, является ли предмет одной из ограниченных лодок
        String itemType = item.getType().name().toLowerCase();
        boolean isRestrictedBoat = restrictedBoats.stream()
                .anyMatch(boat -> boat.equalsIgnoreCase(itemType));

        if (!isRestrictedBoat) return;

        // Проверяем, находится ли игрок в ограниченном мире
        if (!restrictedWorlds.contains(player.getWorld().getName())) return;

        Block clickedBlock = e.getClickedBlock();
        if (clickedBlock == null) return;

        // Проверяем, является ли блок под кликом водой
        if (clickedBlock.getType() != Material.WATER) {
            e.setCancelled(true);
            player.sendMessage(getConfig().getString("deny-message", "§cЗдесь нельзя ставить лодки!"));

            // Опционально: воспроизвести звук ошибки
            // player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
        }
    }
}