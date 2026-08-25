package com.weaponsmp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class WeaponsOnlySMP extends JavaPlugin implements Listener, CommandExecutor {

    private final HashMap<String, Long> cooldowns = new HashMap<>();
    private final HashMap<UUID, String> selectedDimensions = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("giveweapon").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /giveweapon <type>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "crownbreaker" -> player.getInventory().addItem(createItem(Material.NETHERITE_MACE, "§e§lCrown Breaker"));
            case "blasterbow" -> player.getInventory().addItem(createItem(Material.BOW, "§c§lBlaster Bow"));
            case "voidsword" -> player.getInventory().addItem(createItem(Material.NETHERITE_SWORD, "§5§lVoid Sword"));
            case "tidebreaker" -> player.getInventory().addItem(createItem(Material.TRIDENT, "§b§lTide Breaker"));
            case "harpoon" -> player.getInventory().addItem(createItem(Material.FISHING_ROD, "§6§lHarpoon"));
            case "grappler" -> player.getInventory().addItem(createItem(Material.CROSSBOW, "§a§lSpecial Grappler"));
            case "shadowsword" -> player.getInventory().addItem(createItem(Material.DIAMOND_SWORD, "§8§lShadow Sword"));
            case "teleporter" -> player.getInventory().addItem(createItem(Material.COMPASS, "§d§lDimension Teleporter"));
        }
        return true;
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        String name = item.getItemMeta().getDisplayName();

        if (name.contains("Shadow Sword")) {
            if (event.getHand() != EquipmentSlot.HAND) return;
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (p.isSneaking()) {
                    if (checkCooldown(p, "shadow_blast", 30)) {
                        for (Entity e : p.getNearbyEntities(6, 6, 6)) {
                            if (e instanceof LivingEntity target && e != p) {
                                target.setVelocity(new Vector(0, 1.2, 0));
                                p.getWorld().createExplosion(target.getLocation(), 0.0f, false, false);
                                target.setHealth(Math.min(target.getHealth(), 10.0));
                            }
                        }
                        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
                    }
                } else {
                    if (checkCooldown(p, "shadow_levitate", 15)) {
                        for (Entity e : p.getNearbyEntities(5, 5, 5)) {
                            if (e instanceof LivingEntity target && e != p) {
                                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                                target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 1));
                            }
                        }
                        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.0f, 0.5f);
                    }
                }
            }
        } else if (name.contains("Special Grappler")) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (event.getHand() != EquipmentSlot.HAND) return;
                if (checkCooldown(p, "grapple_pull", 60)) {
                    Block targetBlock = p.getTargetBlockExact(100);
                    if (targetBlock != null) {
                        Vector direction = targetBlock.getLocation().toVector().subtract(p.getLocation().toVector()).normalize();
                        p.setVelocity(direction.multiply(3.5));
                        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 2.0f);
                    }
                }
            } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                if (checkCooldown(p, "grapple_regen", 60)) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
                    p.getWorld().playSound(p.getLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, 1.0f, 1.0f);
                }
            }
        } else if (name.contains("Dimension Teleporter")) {
            if (event.getHand() != EquipmentSlot.HAND) return;
            if (p.isSneaking() && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
                String currentDim = selectedDimensions.getOrDefault(p.getUniqueId(), "world");
                String nextDim = currentDim.equals("world") ? "world_nether" : (currentDim.equals("world_nether") ? "world_the_end" : "world");
                selectedDimensions.put(p.getUniqueId(), nextDim);
                p.sendMessage(ChatColor.LIGHT_PURPLE + "Selected Dimension: " + ChatColor.GOLD + nextDim.toUpperCase());
            } else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (checkCooldown(p, "teleport_self", 15)) {
                    World targetWorld = Bukkit.getWorld(selectedDimensions.getOrDefault(p.getUniqueId(), "world"));
                    if (targetWorld != null) teleportRandomly(p, targetWorld);
                }
            }
        } else if (name.contains("Void Sword")) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (event.getHand() != EquipmentSlot.HAND) return;
                if (p.isSneaking()) {
                    if (checkCooldown(p, "void_shift", 50)) {
                        for (Entity e : p.getNearbyEntities(15, 15, 15)) {
                            if (e instanceof LivingEntity target && e != p) {
                                target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 100, 1));
                                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
                            }
                        }
                    }
                } else {
                    for (Entity e : p.getNearbyEntities(8, 8, 8)) {
                        if (e instanceof LivingEntity target && e != p) {
                            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                        }
                    }
                }
            } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                if (checkCooldown(p, "void_dragon_barrage", 180)) {
                    for (int i = 0; i < 8; i++) {
                        Bukkit.getScheduler().runTaskLater(this, () -> {
                            DragonFireball fireball = p.launchProjectile(DragonFireball.class);
                            fireball.setVelocity(p.getLocation().getDirection().multiply(1.8));
                        }, i * 5L);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player p = event.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player targetPlayer)) return;

        if (item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("Dimension Teleporter")) {
            if (p.isSneaking()) {
                if (checkCooldown(p, "teleport_other", 20)) {
                    World targetWorld = Bukkit.getWorld(selectedDimensions.getOrDefault(p.getUniqueId(), "world"));
                    if (targetWorld != null) teleportRandomly(targetPlayer, targetWorld);
                }
            }
        }
    }

    private void teleportRandomly(Player target, World world) {
        int randomX = (int) (Math.random() * 2000) - 1000;
        int randomZ = (int) (Math.random() * 2000) - 1000;
        int randomY = world.getHighestBlockYAt(randomX, randomZ) + 1;
        target.teleport(new Location(world, randomX, randomY, randomZ));
        world.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }

    private boolean checkCooldown(Player player, String key, int seconds) {
        String fullKey = player.getUniqueId() + "_" + key;
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(fullKey)) {
            long last = cooldowns.get(fullKey);
            long pass = (now - last) / 1000;
            if (pass < seconds) {
                player.sendMessage(ChatColor.RED + "Ability Cooldown: " + (seconds - pass) + "s");
                return false;
            }
        }
        cooldowns.put(fullKey, now);
        return true;
    }
  }
          
