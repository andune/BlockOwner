/**
 * 
 */
package com.andune.minecraft.blockowner;

import java.io.IOException;

/**
 * @author andune
 *
 */
public interface ChunkStorage {
    /**
     * Save a chunk to backing I/O storage.
     * 
     * @param chunk
     * @throws IOException
     */
    public void save(Chunk chunk) throws IOException;
    
    /**
     * Load a chunk from backing I/O storage. Note that it is expected
     * the chunk has it's X/Z coordinates set properly and also that any
     * data currently set in the Chunk object is overwritten.
     * 
     * @param chunk
     * @throws IOException
     */
    public void load(Chunk chunk) throws IOException;
}
