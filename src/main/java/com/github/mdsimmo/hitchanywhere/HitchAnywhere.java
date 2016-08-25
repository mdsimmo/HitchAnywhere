package com.github.mdsimmo.hitchanywhere;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;

public class HitchAnywhere extends JavaPlugin implements Listener {

    private static final int MAX_SEARCH = 2;
    private static final Vector OFFSET = new Vector(1, 0.8, 0);
    private final Set<Material> hitchable = new HashSet<>();

    @Override
    public void onEnable () {
        super.onEnable();
        getServer().getPluginManager().registerEvents(this, this);

        FileConfiguration config = getConfig();
        config.addDefault("hitch-blocks", Arrays.asList(
                Material.WOOD.toString(),
                Material.LOG.toString(),
                Material.LOG_2.toString()
        ));
        config.options().copyDefaults(true);
        saveConfig();

        List<String> hitches = config.getStringList("hitch-blocks");
        if (hitches == null)
            return;
        for (String matName : hitches ) {
            Material mat = Material.valueOf(matName);
            hitchable.add(mat);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e) {

        Block b = e.getClickedBlock();
        Player p = e.getPlayer();
        Action a = e.getAction();

        if (a == Action.RIGHT_CLICK_BLOCK) {
            System.out.println(Arrays.deepToString(hitchable.toArray()));
            if (hitchable.contains(b.getType()))
                hitchAnimals(p, b);
        } else if (a == Action.LEFT_CLICK_BLOCK) {
            unhitchAnimals(b);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        unhitchAnimals(e.getBlock());
        for (Block b : e.blockList()) {
            unhitchAnimals(b);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTNTExplode(EntityExplodeEvent e) {
        for (Block b : e.blockList()) {
            unhitchAnimals(b);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        unhitchAnimals(e.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        for (Block b : e.getBlocks()) {
            unhitchAnimals(b);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonRetractEvent e) {
        for (Block b : e.getBlocks()) {
            unhitchAnimals(b);
        }
    }

    private static Set<LivingEntity> getLeashedEntities(Entity player) {
        Set<LivingEntity> entities = new HashSet<>();
        Location l = player.getLocation();
        int x = l.getBlockX();
        int z = l.getBlockZ();
        for (int i = -MAX_SEARCH; i <= MAX_SEARCH; i++) {
            for (int j = -MAX_SEARCH; j <= MAX_SEARCH; j++) {
                l.setX(x + i*16);
                l.setZ(z + j * 16);
                Chunk chunk = l.getChunk();
                for (Entity entity : chunk.getEntities()) {
                    if (entity instanceof LivingEntity) {
                        LivingEntity living = (LivingEntity)entity;
                        if (living.isLeashed()
                                && living.getLeashHolder().equals(player)) {
                            entities.add(living);
                        }
                    }
                }
            }
        }
        return entities;
    }

    private void hitchAnimals(Player p, Block b) {
        Location l = b.getLocation().add(OFFSET);

        Set<LivingEntity> leashed = getLeashedEntities(p);
        if (leashed.isEmpty())
            return;

        ArmorStand stand = (ArmorStand) p.getWorld().spawnEntity(l, EntityType.ARMOR_STAND);
        stand.setInvulnerable(true);
        stand.setMarker(true);
        stand.setAI(false);
        stand.setVisible(false);
        stand.setGravity(false);

        for (LivingEntity entity : leashed)
            entity.setLeashHolder(stand);
    }

    private void unhitchAnimals(Block b) {
        Location l = b.getLocation().add(OFFSET);
        Location sl = l.clone();
        Chunk c = l.getChunk();
        for (Entity entity : c.getEntities()) {
            if (entity instanceof ArmorStand) {
                ArmorStand stand = (ArmorStand)entity;
                stand.getLocation(sl);
                if (!stand.hasAI() && stand.isMarker() && !stand.isVisible() && stand.isInvulnerable()
                        && sl.getBlockX() == l.getBlockX()
                        && sl.getBlockY() == l.getBlockY()
                        && sl.getBlockZ() == l.getBlockZ()) {
                    // we can be fairly sure now that this is a stand we want
                    // to remove
                    stand.remove();
                }
            }
        }
    }

}
