/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2013 Andune (andune.alleria@gmail.com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
/**
 * 
 */
package com.andune.blockowner;

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
