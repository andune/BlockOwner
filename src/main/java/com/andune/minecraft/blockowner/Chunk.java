/**
 * 
 */
package com.andune.minecraft.blockowner;

import java.util.Formatter;


import com.andune.minecraft.blockowner.util.Debug;
import com.carrotsearch.hppc.IntShortOpenHashMap;

/**
 * @author andune
 *
 */
public class Chunk {
    protected final int x;
    protected final int z;
    protected final String world;
    protected final long chunkKey;
    
    private final Owners owners;
    protected IntShortOpenHashMap map = new IntShortOpenHashMap(100);
    
    public Chunk(String world, int x, int z, Owners owners) {
    	this.world = world;
        this.x = x;
        this.z = z;
        this.owners = owners;
        
	    this.chunkKey = (x << 16) | (z & 0xFFFF);
    }
    
    public String getOwner(int x, int y, int z) {
        short value = 0;

        // same bit pattern Bukkit uses for 32-bit chunk keys
        int key = (y&0xFF) << 8 | (z&0xF) << 4 | (x&0xF);
        if (map.containsKey(key))
            value = map.lget();

        String owner = null;
        if( value != 0 )
            owner = owners.getOwnerById(value);
        
        if( Debug.getInstance().isDebug() ) {
            Formatter f = new Formatter();
	        f.format("chunk getOwner(): chunk {%d,%d} location {%d,%d,%d,key=%x} owner is %d (%s)",
	        		this.x, this.z, x, y, z, key, value, owner);
	        Debug.getInstance().debug(f.toString());
	        f.close();
        }

        return owner;
    }
    
    public void setOwner(int x, int y, int z, String owner) {
        short ownerId = 0;
        if( owner != null )
            ownerId = owners.getOrCreateOwner(owner);

        // same bit pattern Bukkit uses for 32-bit chunk keys
        int key = (y&0xFF) << 8 | (z&0xF) << 4 | (x&0xF);
        
        if( owner != null ) {
            map.put(key, ownerId);
            if( Debug.getInstance().isDebug() ) {
                Formatter f = new Formatter();
		        f.format("chunk owner chunk {%d,%d} location {%d,%d,%d,key=%x} set to %d",
		        		this.x, this.z, x, y, z, key, ownerId);
		        Debug.getInstance().debug(f.toString());
		        f.close();
            }
        }
        else {
            map.remove(key);        // remove owner
            Debug.getInstance().debug("chunk owner {",x,",",y,",",z,"} removed");
        }
    }
    
    public String toString() {
        return "{Chunk "+x+","+z+"}";
    }
}
