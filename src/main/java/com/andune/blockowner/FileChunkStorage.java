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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Formatter;

import com.andune.minecraft.commonlib.Logger;
import com.andune.minecraft.commonlib.LoggerFactory;
import org.bukkit.plugin.Plugin;

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
    private final Logger log = LoggerFactory.getLogger(FileChunkStorage.class);
	private final File worldContainer;

	public FileChunkStorage(Plugin plugin) {
		this.worldContainer = plugin.getServer().getWorldContainer();
	}
	
    /**
     * Return the region directory and create it if it doesn't exist.
     *
     * @param worldName
     * @param chunkX
     * @param chunkZ
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
        	log.debug("load on chunk {}, no file exists, initializing empty dataset", chunk);
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
                if( log.isDebugEnabled() ) {
                    Formatter format = new Formatter();
                    format.format("loaded chunk{%d,%d} owner %d for key %x",
                    		chunk.x, chunk.z, value, key);
    		       log.debug(format.toString());
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
