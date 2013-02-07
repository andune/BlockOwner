/**
 * 
 */
package com.andune.minecraft.blockowner;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Formatter;

import org.bukkit.plugin.Plugin;

import com.andune.minecraft.blockowner.util.Debug;
import com.carrotsearch.hppc.IntShortOpenHashMap;

/**
 * Implementation of ChunkStorage that uses the file system.
 * 
 * This is for TESTING ONLY. It is expected that on a production server,
 * thousands of chunk files will cause I/O delays, the same as the original
 * MC server did when it had one-file-per-chunk. A solution is to group
 * files into regions like MC does now, so if someone wants to contribute
 * such an implementation, it could be used on production servers.
 * 
 * @author andune
 *
 */
public class FileChunkStorage implements ChunkStorage {
	private final File worldContainer;
	public FileChunkStorage(Plugin plugin) {
		this.worldContainer = plugin.getServer().getWorldContainer();
	}
	
    /**
     * Return the region directory and create it if it doesn't exist.
     * 
     * @param chunkX
     * @param chunkY
     * @return
     */
    private File getRegionDirectory(String worldName, int chunkX, int chunkZ) {
        int regionX = chunkX >> 4;
        int regionZ = chunkZ >> 4;
        
        File regionDirectory = new File(worldContainer, worldName+"/blockOwner/"+regionX+"_"+regionZ);
        if( !regionDirectory.exists() ) {
            regionDirectory.mkdirs();
        }
        
        return regionDirectory;
    }

    @Override
    public void save(Chunk chunk) throws IOException {
        File regionDirectory = getRegionDirectory(chunk.world, chunk.x, chunk.z);
        File f = new File(regionDirectory, chunk.x+"_"+chunk.z);
        DataOutputStream os = null;
        
        try {
            os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
            os.writeInt(chunk.map.assigned);
            
            final int [] keys = chunk.map.keys;
            final short [] values = chunk.map.values;
            final boolean [] states = chunk.map.allocated;

            for (int i = 0; i < states.length; i++)
            {
                if (states[i]) {
                    os.writeInt(keys[i]);
                    os.writeShort(values[i]);
                }
            }

            os.flush();
        }
        finally {
            if( os != null )
                os.close();
        }
    }

    @Override
    public void load(Chunk chunk) throws IOException {
        File regionDirectory = getRegionDirectory(chunk.world, chunk.x, chunk.z);
        File f = new File(regionDirectory, chunk.x+"_"+chunk.z);
        
        // if no file exists, just initialize a small-capacity map
        if( !f.exists() ) {
        	Debug.getInstance().debug("load on chunk "+chunk+", no file exists, initializing empty dataset");
            chunk.map = new IntShortOpenHashMap(100);
            return;
        }
        
        DataInputStream is = null;

        try {
            is = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
            
            // first 32 bits represent the size of the map
            int available = is.readInt();

            // we round up by 30% to account for .75 load factor in hash.
            // this initializes the map at less than the load factor with
            // some extra room for growth before needing a clone & grow
            chunk.map = new IntShortOpenHashMap((int) (available*1.3));
            
            while(is.available() > 0) {
                int key = is.readInt();
                short value = is.readShort();
                chunk.map.put(key, value);
                if( Debug.getInstance().isDebug() ) {
                    Formatter format = new Formatter();
                    format.format("loaded chunk{%d,%d} owner %d for key %x",
                    		chunk.x, chunk.z, value, key);
    		        Debug.getInstance().debug(format.toString());
    		        format.close();
                }
            }
        }
        finally {
            if( is != null )
                is.close();
        }
    }

}
