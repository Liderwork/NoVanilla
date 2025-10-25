package me.yourname.novanilla;

import org.bukkit.GameRule;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.Keyed;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

// Crafter event can be absent in some Paper API builds; we avoid hard dependency.
// import io.papermc.paper.event.block.CrafterCraftEvent;

import java.util.*;

public final class NoVanilla extends JavaPlugin implements Listener {

    private static final Set<InventoryType> BLOCKED_TYPES = EnumSet.of(
            InventoryType.CRAFTING, InventoryType.WORKBENCH,
            InventoryType.FURNACE, InventoryType.SMOKER, InventoryType.BLAST_FURNACE,
            InventoryType.ANVIL, InventoryType.GRINDSTONE, InventoryType.SMITHING,
            InventoryType.STONECUTTER, InventoryType.CARTOGRAPHY, InventoryType.LOOM,
            InventoryType.BREWING, InventoryType.CRAFTER
    );

    private final Map<UUID, Long> lastNotify = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);

        Iterator<Recipe> it = getServer().recipeIterator();
        while (it.hasNext()) {
            Recipe r = it.next();
            if (r instanceof Keyed keyed) {
                NamespacedKey key = keyed.getKey();
                if ("minecraft".equals(key.getNamespace())) {
                    it.remove();
                }
            }
        }

        for (World w : getServer().getWorlds()) {
            w.setGameRule(GameRule.DO_LIMITED_CRAFTING, true);
        }

        getLogger().info("NoVanilla: enabled (Paper 1.21.1), recipes removed, UIs blocked.");
    }

    private boolean bypass(HumanEntity who) {
        return who instanceof Player p && (p.isOp() || p.hasPermission("novanilla.bypass"));
    }

    private String actionName(InventoryType type) {
        return switch (type) {
            case CRAFTING, WORKBENCH -> "крафт";
            case SMITHING -> "кузница";
            case ANVIL -> "наковальня";
            case GRINDSTONE -> "точило";
            case STONECUTTER -> "камнерез";
            case CARTOGRAPHY -> "картография";
            case LOOM -> "ткацкий станок";
            case BREWING -> "варка";
            case FURNACE, SMOKER, BLAST_FURNACE -> "переплавка";
            case CRAFTER -> "автокрафт";
            default -> "действие";
        };
    }

    private void notifyBlocked(Player p, String action) {
        if (p == null) return;
        long now = System.currentTimeMillis();
        long cooldown = getConfig().getLong("notify.cooldown-ms", 1500L);
        long last = lastNotify.getOrDefault(p.getUniqueId(), 0L);
        if (now - last < cooldown) return;
        lastNotify.put(p.getUniqueId(), now);

        String template = getConfig().getString("notify.message", "&cВанильный {action} отключён");
        String text = template.replace("{action}", action);

        boolean useActionbar = getConfig().getBoolean("notify.actionbar", true);
        boolean useChat = getConfig().getBoolean("notify.chat", false);

        Component comp = LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        if (useActionbar) p.sendActionBar(comp);
        if (useChat) p.sendMessage(comp);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCraft(CraftItemEvent e) {
        if (bypass(e.getWhoClicked())) return;
        e.setCancelled(true);
        if (e.getWhoClicked() instanceof Player p) {
            notifyBlocked(p, "крафт");
        }
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        if (!e.getViewers().isEmpty() && bypass(e.getView().getPlayer())) return;
        e.getInventory().setResult(null);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSmith(SmithItemEvent e) {
        if (bypass(e.getWhoClicked())) return;
        e.setCancelled(true);
        if (e.getWhoClicked() instanceof Player p) notifyBlocked(p, "кузница");
    }

    @EventHandler
    public void onPrepareSmith(PrepareSmithingEvent e) {
        if (bypass(e.getView().getPlayer())) return;
        e.setResult(null);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent e) {
        if (bypass(e.getView().getPlayer())) return;
        e.setResult(null);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPrepareGrind(PrepareGrindstoneEvent e) {
        if (bypass(e.getView().getPlayer())) return;
        e.setResult(null);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (bypass(e.getWhoClicked())) return;
        Inventory top = e.getView().getTopInventory();
        InventoryType type = top.getType();
        if (!BLOCKED_TYPES.contains(type)) return;
        if (e.getClickedInventory() == top) {
            e.setCancelled(true);
            if (e.getSlotType() == SlotType.RESULT && e.getWhoClicked() instanceof Player p) {
                notifyBlocked(p, actionName(type));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockCook(BlockCookEvent e) { e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onBrew(BrewEvent e) { e.setCancelled(true); }

    // Crafter handler removed to avoid compile error if API doesn't expose it yet.
    // @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    // public void onCrafter(CrafterCraftEvent e) { e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryMove(InventoryMoveItemEvent e) {
        InventoryType src = e.getSource().getType();
        InventoryType dst = e.getDestination().getType();
        if (BLOCKED_TYPES.contains(src) || BLOCKED_TYPES.contains(dst)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDiscover(PlayerRecipeDiscoverEvent e) { e.setCancelled(true); }
}
