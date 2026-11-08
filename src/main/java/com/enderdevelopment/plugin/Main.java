package com.enderdevelopment.plugin;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class Main extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private static final String PREFIX = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Maces" + ChatColor.DARK_GRAY + "] ";
    private static final Map<String, MaceTier> TIERS = Map.of(
            "wood", new MaceTier("Wood", Material.WOODEN_SWORD, 7.0, ChatColor.YELLOW, 1),
            "stone", new MaceTier("Stone", Material.STONE_SWORD, 9.0, ChatColor.GRAY, 2),
            "iron", new MaceTier("Iron", Material.IRON_SWORD, 12.0, ChatColor.WHITE, 3),
            "diamond", new MaceTier("Diamond", Material.DIAMOND_SWORD, 16.0, ChatColor.AQUA, 4),
            "netherite", new MaceTier("Netherite", Material.NETHERITE_SWORD, 21.0, ChatColor.DARK_RED, 5),
            "infinity", new MaceTier("Infinity", Material.NETHERITE_SWORD, 30.0, ChatColor.LIGHT_PURPLE, 6));
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private NamespacedKey tierKey;
    private NamespacedKey unlockedTierKey;

    @Override
    public void onEnable() {
        tierKey = new NamespacedKey(this, "mace_tier");
        unlockedTierKey = new NamespacedKey(this, "unlocked_mace_tier");
        Bukkit.getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("custommace")).setExecutor(this);
        Objects.requireNonNull(getCommand("custommace")).setTabCompleter(this);
        registerRecipes();
        getLogger().info("CustomMaces enabled with six mace tiers.");
    }

    @Override
    public void onDisable() {
        cooldowns.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || !TIERS.containsKey(args[0].toLowerCase(Locale.ROOT))) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + "Usage: /" + label + " <wood|stone|iron|diamond|netherite|infinity> [player]");
            return true;
        }
        Player target;
        if (args.length > 1) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(PREFIX + ChatColor.RED + "That player is not online.");
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(PREFIX + ChatColor.RED + "Console must specify a player.");
            return true;
        }
        MaceTier tier = TIERS.get(args[0].toLowerCase(Locale.ROOT));
        target.getInventory().addItem(createMace(tier));
        sender.sendMessage(PREFIX + ChatColor.GREEN + "Gave " + tier.displayName + " Mace to " + target.getName() + ".");
        if (sender != target) {
            target.sendMessage(PREFIX + ChatColor.GREEN + "You received the " + tier.displayName + " Mace.");
        }
        return true;
    }

    private void registerRecipes() {
        registerTierRecipe("wood", Material.OAK_LOG, Material.MACE);
        registerTierRecipe("stone", Material.COBBLESTONE, customChoice("wood"));
        registerTierRecipe("iron", Material.IRON_BLOCK, customChoice("stone"));
        registerTierRecipe("diamond", Material.DIAMOND_BLOCK, customChoice("iron"));
        registerTierRecipe("netherite", Material.NETHERITE_INGOT, customChoice("diamond"));
        registerTierRecipe("infinity", Material.NETHER_STAR, customChoice("netherite"));
    }

    private void registerTierRecipe(String tierId, Material ingredient, Object base) {
        MaceTier tier = TIERS.get(tierId);
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, tierId + "_mace_recipe"), createMace(tier));
        recipe.shape("MMM", "MBM", " S ");
        recipe.setIngredient('M', ingredient);
        if (base instanceof Material material) {
            recipe.setIngredient('B', material);
        } else {
            recipe.setIngredient('B', (RecipeChoice) base);
        }
        recipe.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(recipe);
    }

    private RecipeChoice customChoice(String tierId) {
        return new RecipeChoice.ExactChoice(createMace(TIERS.get(tierId)));
    }

    private ItemStack createMace(MaceTier tier) {
        ItemStack item = new ItemStack(Material.MACE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(tier.color + ChatColor.BOLD + tier.displayName + " Mace");
        meta.setLore(java.util.List.of(
                ChatColor.GRAY + "Attack Damage: " + ChatColor.WHITE + String.format("%.1f", tier.damage),
                ChatColor.GRAY + "Power: " + ChatColor.WHITE + tier.powerDescription()));
        meta.getPersistentDataContainer().set(tierKey, PersistentDataType.STRING, tier.id());
        AttributeModifier modifier = new AttributeModifier(new NamespacedKey(this, tier.id() + "_damage"), tier.damage - 1.0,
                AttributeModifier.Operation.ADD_NUMBER);
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifier);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMaceHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        MaceTier tier = getTier(player.getInventory().getItemInMainHand());
        if (tier == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long readyAt = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now < readyAt) {
            return;
        }
        cooldowns.put(player.getUniqueId(), now + tier.cooldownMillis());
        applyPower(player, target, tier);
    }

    private void applyPower(Player player, LivingEntity target, MaceTier tier) {
        switch (tier.id()) {
            case "wood" -> {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0));
                player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE, 1.0f, 1.2f);
            }
            case "stone" -> {
                target.setVelocity(target.getVelocity().add(new Vector(0, 0.65, 0)));
                player.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 0.8f);
            }
            case "iron" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 0));
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.7f, 1.5f);
            }
            case "diamond" -> {
                target.getWorld().strikeLightningEffect(target.getLocation());
                player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.7f);
            }
            case "netherite" -> {
                target.setFireTicks(Math.max(target.getFireTicks(), 100));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0));
                player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);
            }
            case "infinity" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 1));
                for (Entity nearby : target.getNearbyEntities(3.0, 2.0, 3.0)) {
                    if (nearby instanceof LivingEntity living && nearby != player) {
                        living.damage(4.0, player);
                        living.setVelocity(living.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(0.8).setY(0.35));
                    }
                }
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.25f, 1.8f);
            }
            default -> { }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) {
            cooldowns.remove(victim.getUniqueId());
            return;
        }

        MaceTier killerTier = getTier(killer.getInventory().getItemInMainHand());
        MaceTier victimTier = findMace(victim);
        if (killerTier == null || victimTier == null || victimTier.number() < killerTier.number()
                || victimTier.number() > killerTier.number() + 1) {
            cooldowns.remove(victim.getUniqueId());
            return;
        }

        unlockTier(killer, victimTier);
        event.getDrops().removeIf(item -> getTier(item) != null);
        killer.getInventory().addItem(createMace(victimTier));
        killer.sendMessage(PREFIX + ChatColor.GREEN + "You earned the " + victimTier.displayName
                + " Mace by defeating " + victim.getName() + ".");
        victim.sendMessage(PREFIX + ChatColor.RED + "Your " + victimTier.displayName + " Mace was claimed by "
                + killer.getName() + ".");
        cooldowns.remove(victim.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMaceCraft(CraftItemEvent event) {
        MaceTier tier = getTier(event.getRecipe().getResult());
        if (tier == null || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!hasUnlockedTier(player, tier)) {
            event.setCancelled(true);
            player.sendMessage(PREFIX + ChatColor.RED + "You must defeat a player using a " + tier.displayName
                    + " Mace or the tier directly below it before crafting this mace.");
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        cooldowns.remove(event.getEntity().getUniqueId());
    }

    private MaceTier findMace(Player player) {
        MaceTier mainHand = getTier(player.getInventory().getItemInMainHand());
        if (mainHand != null) {
            return mainHand;
        }
        for (ItemStack item : player.getInventory().getContents()) {
            MaceTier tier = getTier(item);
            if (tier != null) {
                return tier;
            }
        }
        return null;
    }

    private void unlockTier(Player player, MaceTier tier) {
        player.getPersistentDataContainer().set(unlockedTierKey, PersistentDataType.INTEGER,
                Math.max(tier.number(), getUnlockedTier(player)));
    }

    private boolean hasUnlockedTier(Player player, MaceTier tier) {
        return getUnlockedTier(player) >= tier.number();
    }

    private int getUnlockedTier(Player player) {
        Integer unlocked = player.getPersistentDataContainer().get(unlockedTierKey, PersistentDataType.INTEGER);
        return unlocked == null ? 1 : unlocked;
    }

    private MaceTier getTier(ItemStack item) {
        if (item == null || item.getType() != Material.MACE || !item.hasItemMeta()) {
            return null;
        }
        String id = item.getItemMeta().getPersistentDataContainer().get(tierKey, PersistentDataType.STRING);
        return id == null ? null : TIERS.get(id);
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return TIERS.keySet().stream().filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT))).sorted().toList();
        }
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(value -> value.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).sorted().toList();
        }
        return java.util.List.of();
    }

    private record MaceTier(String displayName, Material icon, double damage, ChatColor color, int number) {
        private String id() { return displayName.toLowerCase(Locale.ROOT); }
        private long cooldownMillis() { return number == 1 ? 1200L : number == 2 ? 1500L : number == 3 ? 1800L : number == 4 ? 2200L : number == 5 ? 2800L : 3500L; }
        private String powerDescription() {
            return switch (id()) {
                case "wood" -> "Stuns enemies with Slowness";
                case "stone" -> "Launches enemies upward";
                case "iron" -> "Grants temporary Resistance";
                case "diamond" -> "Calls a lightning strike";
                case "netherite" -> "Ignites and weakens enemies";
                case "infinity" -> "Empowers you and damages nearby enemies";
                default -> "Unknown power";
            };
        }
    }
}
