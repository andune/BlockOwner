/**
 * 
 */
package com.andune.minecraft.blockowner;

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
