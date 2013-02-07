/**
 * 
 */
package com.andune.minecraft.blockowner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * @author andune
 *
 */
public class Owners {
	private final Map<Short, String> ownerIdMap = new HashMap<Short, String>();
	private final Map<String, Short> ownerNameMap = new HashMap<String, Short>();
	private final YamlConfiguration config;
	private final File saveFile;
	private short maxId=0;

	public Owners(YamlConfiguration config, File saveFile) {
		this.config = config;
		this.saveFile = saveFile;
	}
	
	public void loadConfig() throws FileNotFoundException, IOException, InvalidConfigurationException {
		if( saveFile.exists() ) {
			config.load(saveFile);
	
			Set<String> keys = config.getKeys(false);
			if( keys == null )
				return;
			
			for(String key : keys) {
				short id = Short.valueOf(key);
				if( id > maxId )
					maxId = id;
				String ownerName = config.getString(key);
				
				ownerIdMap.put(id, ownerName);
				ownerNameMap.put(ownerName, id);
			}
		}
	}

    /**
     * Given a 16-bit owner id, return the owner string associated with
     * that owner.
     * 
     * @param id
     * @return
     */
    public String getOwnerById(short id) {
    	return ownerIdMap.get(id);
    }
    
    /**
     * Given an owner string, return the owner id.
     * 
     * @param owner
     * @return
     */
    public short getOrCreateOwner(String owner) {
    	Short id = ownerNameMap.get(owner);
    	if( id == null ) {
    		id = ++maxId;
    		ownerIdMap.put(id, owner);
    		ownerNameMap.put(owner, id);
    		config.set(""+id, owner);

    		// TODO: at some point, defer saves to every 5 minutes or something
    		try {
    			config.save(saveFile);
    		}
    		catch(IOException e) {
    			e.printStackTrace();
    		}
    	}
        return id;
    }
}
