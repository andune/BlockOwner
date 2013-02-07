/**
 * 
 */
package com.andune.minecraft.blockowner;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * @author andune
 *
 */
public class BlockListener implements Listener {
	private final GlobalChunkManager manager;
	
	public BlockListener(final GlobalChunkManager manager) {
		this.manager = manager;
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
	    // break event clears previous owner
	    manager.setBlockOwner(event.getBlock(), null);
	}
	
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
        manager.setBlockOwner(event.getBlock(), event.getPlayer().getName());
	}
}
