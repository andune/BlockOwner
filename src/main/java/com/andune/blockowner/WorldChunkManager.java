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

import com.andune.minecraft.commonlib.Logger;
import com.andune.minecraft.commonlib.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;

/** Class to do stuff as chunks go in and out of memory.
 * 
 * @author andune
 *
 */
public class WorldChunkManager {
    private Logger log = LoggerFactory.getLogger(WorldChunkManager.class);
    
	private final ChunkStorage chunkStorage;
	private final String world;
    private final Owners owners;
	private final HashMap<Long, Chunk> chunkMap = new HashMap<Long, Chunk>(1000);
	
	public WorldChunkManager(String world, ChunkStorage chunkStorage, Owners owners) {
	    this.world = world;
	    this.chunkStorage = chunkStorage;
	    this.owners = owners;
	}
	
	/**
	 * The world that this WorldChunkManager is responsible for.
	 * 
	 * @return
	 */
	public String getWorld() {
	    return world;
	}

    public boolean isChunkLoaded(final int x, final int z) {
        final long chunkKey = (x << 16) | (z & 0xFFFF);
        return chunkMap.containsKey(chunkKey);
    }
	
	public void loadChunk(final int x, final int z) {
        final long chunkKey = (x << 16) | (z & 0xFFFF);
        if( chunkMap.containsKey(chunkKey) ) {
    	    log.debug("chunk load skipped for {},{},{}: chunk already loaded", world, x, z);
        	return;
        }

	    log.debug("loading chunk {},{},{}", world, x, z);
	    Chunk chunk = new Chunk(world, x, z, owners);
	    try {
	        chunkStorage.load(chunk);
	        chunkMap.put(chunkKey, chunk);
	    }
	    catch(IOException e) {
	        log.warn("Caught exception loading chunk "+x+","+z, e);
	    }
	}
	
	public void unloadChunk(final int x, final int z) {
        long chunkKey = (x << 16) | (z & 0xFFFF);
        Chunk chunk = chunkMap.remove(chunkKey);
        if( chunk != null ) {
            try {
                chunkStorage.save(chunk);
            }
            catch(IOException e) {
                log.warn("Caught exception saving chunk "+x+","+z, e);
            }
        }
	}
	
	public void setBlockOwner(int blockX, int blockY, int blockZ, String owner) {
        final int chunkX = blockX >> 4;
        final int chunkZ = blockZ >> 4;
        final long chunkKey = (chunkX << 16) | (chunkZ & 0xFFFF);
        log.debug("setBlockOwner chunkKey={}, owner={}", chunkKey, owner);
        
        Chunk chunk = chunkMap.get(chunkKey);
        if( chunk != null ) {
            chunk.setOwner(blockX, blockY, blockZ, owner);
            log.debug("chunk owner {{},{},{}} set to {}", blockX, blockY, blockZ, owner);
        }
        else {
            log.debug("No chunk loaded for block {{},{},{}}", blockX, blockY, blockZ);
        }
	}
	
	public String getBlockOwner(int blockX, int blockY, int blockZ) {
	    String owner = null;
	    
	    int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
	    long chunkKey = (chunkX << 16) | (chunkZ & 0xFFFF);
        log.debug("getBlockOwner chunkKey={}", chunkKey);
        
        Chunk chunk = chunkMap.get(chunkKey);
        if( chunk != null ) {
            owner = chunk.getOwner(blockX, blockY, blockZ);
        }
        else {
            log.debug("No chunk loaded for block {{},{},{}}", blockX, blockY, blockZ);
        }
        
        return owner;
	}
	
	/**
	 * For testing use only. Production use would have to consider thread
	 * safety.
	 */
	public void saveAll() {
	    for(Chunk chunk : chunkMap.values()) {
	        try {
	            log.debug("saving chunk {},{}", world, chunk);
	            chunkStorage.save(chunk);
	        }
	        catch(IOException e) {
	            log.warn("Error saving chunk "+chunk, e);
	        }
	    }
	}
}
