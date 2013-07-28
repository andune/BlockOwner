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

import java.util.Formatter;


import com.andune.minecraft.commonlib.Logger;
import com.andune.minecraft.commonlib.LoggerFactory;
import com.carrotsearch.hppc.IntShortOpenHashMap;

/**
 * @author andune
 *
 */
public class Chunk {
    private final Logger log = LoggerFactory.getLogger(Chunk.class);

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
        
        if( log.isDebugEnabled() ) {
            Formatter f = new Formatter();
	        f.format("chunk getOwner(): chunk {%d,%d} location {%d,%d,%d,key=%x} owner is %d (%s)",
	        		this.x, this.z, x, y, z, key, value, owner);
	       log.debug(f.toString());
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
            if( log.isDebugEnabled() ) {
                Formatter f = new Formatter();
		        f.format("chunk owner chunk {%d,%d} location {%d,%d,%d,key=%x} set to %d",
		        		this.x, this.z, x, y, z, key, ownerId);
		        log.debug(f.toString());
		        f.close();
            }
        }
        else {
            map.remove(key);        // remove owner
            log.debug("chunk owner {{},{},{}} removed", x, y, z);
        }
    }
    
    public String toString() {
        return "{Chunk "+x+","+z+"}";
    }
}
