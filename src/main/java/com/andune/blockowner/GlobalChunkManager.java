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

import java.util.HashMap;

import com.andune.minecraft.commonlib.Logger;
import com.andune.minecraft.commonlib.LoggerFactory;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

/**
 * @author andune
 *
 */
public class GlobalChunkManager implements Listener {
    private final Logger log = LoggerFactory.getLogger(GlobalChunkManager.class);
    private final HashMap<String, WorldChunkManager> worldManagers = new HashMap<String, WorldChunkManager>(10);
    private final ChunkStorage chunkStorage;
    private final Owners owners;
    
    /**
     * 
     * @param chunkStorage the chunkStorage system to use
     * @param owners
     */
    public GlobalChunkManager(ChunkStorage chunkStorage, Owners owners) {
        this.chunkStorage = chunkStorage;
        this.owners = owners;
        
        for(World w : Bukkit.getWorlds()) {
            final String worldName = w.getName();
            worldManagers.put(worldName, new WorldChunkManager(worldName, chunkStorage, owners));
        }
    }
    
    @EventHandler(priority=EventPriority.MONITOR)
    public void onWorldLoad(final WorldLoadEvent e) {
        final String worldName = e.getWorld().getName();
        if( worldManagers.get(worldName) == null ) {
            worldManagers.put(worldName, new WorldChunkManager(worldName, chunkStorage, owners));
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onWorldUnload(final WorldUnloadEvent e) {
        worldManagers.remove(e.getWorld().getName());
    }
    
    public void chunkLoaded(String worldName, int x, int z) {
        final WorldChunkManager wcm = worldManagers.get(worldName);
        if( wcm != null ) {
            wcm.loadChunk(x, z);
        }
    }
    
    /**
     * For some reason, Bukkit can't be reliably counted on to call ChunkLoadEvent
     * for every chunk in the world - maybe spawn chunks are loaded before plugins
     * get a chance to load. So we have this method to allow us to make sure
     * surrounding chunks are loaded. The loadChunk method exits quickly if the
     * chunk is already loaded, so this is very low overhead if all the chunks
     * are already in memory. 
     * 
     * @param worldName
     * @param x
     * @param z
     */
    public void loadSurroundingChunks(String worldName, int x, int z) {
    	int radius = 7;		// tie to actual Bukkit radius later
    	
    	long startTime = System.currentTimeMillis();
    	int chunks=0;
        final WorldChunkManager wcm = worldManagers.get(worldName);
        if( wcm != null ) {
        	for(int i=x-radius; i < x+radius; i++) {
        		for(int j=z-radius; j < z+radius; j++) {
                    wcm.loadChunk(i, j);
                    chunks++;
        		}
        	}
        }
    	long endTime = System.currentTimeMillis();
    	log.debug("Loaded {} chunk data in {} milliseconds", chunks, (endTime - startTime));
    }
    
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        final String worldName = event.getWorld().getName();
        chunkLoaded(worldName, event.getChunk().getX(), event.getChunk().getZ());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        final String worldName = event.getWorld().getName();
        final WorldChunkManager wcm = worldManagers.get(worldName);
        if( wcm != null ) {
            wcm.unloadChunk(event.getChunk().getX(), event.getChunk().getZ());
        }
    }
    
    public void setBlockOwner(Block b, String owner) {
        final String worldName = b.getWorld().getName();
        final WorldChunkManager wcm = worldManagers.get(worldName);
        if( wcm != null ) {
            wcm.setBlockOwner(b.getX(), b.getY(), b.getZ(), owner);
        }
    }
    
    public String getBlockOwner(String world, int x, int y, int z) {
    	WorldChunkManager wcm = worldManagers.get(world);
        if( wcm != null ) {
            return wcm.getBlockOwner(x, y, z);
        }
        else
            return null;
    }
    public String getBlockOwner(Block b) {
        return getBlockOwner(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
    }
    
    /**
     * For testing only. Production use would have to consider thread safety.
     */
    public void saveAll() {
        for(WorldChunkManager wcm : worldManagers.values()) {
            log.debug("saving world {}", wcm.getWorld());
            wcm.saveAll();
        }
    }
}
