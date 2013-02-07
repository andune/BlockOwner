/**
 * 
 */
package com.andune.minecraft.blockowner;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

/**
 * @author andune
 *
 */
public class PlayerListener implements Listener {
	private GlobalChunkManager globalChunkManager;
	
	public PlayerListener(GlobalChunkManager globalChunkManager) {
		this.globalChunkManager = globalChunkManager;
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerLogin(PlayerLoginEvent event) {
		org.bukkit.Chunk chunk = event.getPlayer().getLocation().getChunk();
		globalChunkManager.loadSurroundingChunks(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		org.bukkit.Chunk chunk = event.getPlayer().getLocation().getChunk();
		globalChunkManager.loadSurroundingChunks(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		ItemStack item = event.getItem();
		if( item != null && item.getTypeId() == 274 ) {
			Block b = event.getClickedBlock();
			String owner = globalChunkManager.getBlockOwner(b);
			event.getPlayer().sendMessage("Owner for block at location "
					+b.getX()+","+b.getY()+","+b.getZ()+" is "+ owner);
		}
	}
}
