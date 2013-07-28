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
package com.andune.blockowner.blocklogger;

import com.andune.blockowner.Owners;
import com.andune.blockowner.WorldChunkManager;
import com.andune.minecraft.commonlib.Logger;
import com.andune.minecraft.commonlib.LoggerFactory;
import de.diddiz.LogBlock.QueryParams;
import de.diddiz.LogBlock.config.Config;
import de.diddiz.LogBlock.config.WorldConfig;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import static de.diddiz.LogBlock.config.Config.*;

/**
 * Class which scans through LogBlock tables to import block owners.
 *
 * @author andune
 */
public class LogBlock {
    private final Logger log = LoggerFactory.getLogger(LogBlock.class);
    private final int PROCESS_ROWS = 1000;  // how many rows to process at one time

    private HashMap<String, Long> oldestImportIds;
    private final Plugin plugin;
    private YamlConfiguration config;
    private final File configFile;
    private de.diddiz.LogBlock.LogBlock logBlockPlugin;

    public LogBlock(Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder()+"/logblock.yml");
        Plugin p = plugin.getServer().getPluginManager().getPlugin("LogBlock");
        if( p != null )
            this.logBlockPlugin = (de.diddiz.LogBlock.LogBlock) p;
    }

    public void loadConfig() throws IOException, InvalidConfigurationException {
        config = new YamlConfiguration();
        if( configFile.exists() ) {
            config.load(configFile);
        }

        ConfigurationSection section = config.getConfigurationSection("worlds");
        if( section != null ) {
            for (String world : section.getKeys(false)) {
                oldestImportIds.put(world, section.getLong(world));
            }
        }
    }

    public void saveConfig() throws IOException {
        config.save(configFile);
    }

    public void startImport(String world) {
        ImportRunner runner = new ImportRunner(world, oldestImportIds.get(world));
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runner);
    }

    private synchronized void updateWorldId(String world, Long id) {
        oldestImportIds.put(world, id);
    }

    private class ImportRunner implements Runnable {
        private String world;
        private long currentId;
        private World bukkitWorld;
        private WorldChunkManager wcm;
        private HashMap<Integer, String> logBlockOwnerIds;
        // keep track of chunks that we load as we are importing, so we can save
        // them to disk and then unload them when we are done.
        private HashSet<Long> loadedChunks;

        public ImportRunner(String world, long startId) {
            this.world = world;
            this.currentId = startId;
        }

        public void run() {
            WorldConfig config = getWorldConfig(world);
            String table = config.table;

            Connection con = logBlockPlugin.getConnection();
            long lowestRow = currentId - PROCESS_ROWS;
            try {
                Statement statement = con.createStatement();

                if( lowestRow < 0 )
                    lowestRow = 0;

                // load up owner ids for fast in-memory lookup
                String query = "SELECT playerid, playername FROM `lb-players`";
                ResultSet rs = statement.executeQuery(query);
                while( rs.next() ) {
                    int id = rs.getInt(1);
                    String name = rs.getString(2);
                    logBlockOwnerIds.put(id, name);
                }
                rs.close();

                query = "SELECT id,playerid,x,y,z,type"
                        + " FROM `" + table + "`"
                        + " WHERE id < " + currentId + " AND id >= " + lowestRow;
                rs = statement.executeQuery(query);
                while( rs.next() ) {
                    int column = 1;
                    long id = rs.getLong(column);
                    int playerId = rs.getInt(++column);
                    int x = rs.getInt(++column);
                    int y = rs.getInt(++column);
                    int z = rs.getInt(++column);
                    int type = rs.getInt(++column);

                    // if type is 0, that means an air block was placed. skip it
                    if( type == 0 )
                        continue;

                    int chunkX = x >> 4;
                    int chunkZ = z >> 4;

                    if( !wcm.isChunkLoaded(chunkX, chunkZ) ) {
                        wcm.loadChunk(chunkX, chunkZ);
                        final long chunkKey = (x << 16) | (z & 0xFFFF);
                        loadedChunks.add(Long.valueOf(chunkKey));
                    }

                    // if we already have an owner for this block, skip it
                    if( wcm.getBlockOwner(x, y, z) != null )
                        continue;

                    // if we get here, we have no current owner and the block
                    // is an ownable block, so this is the current owner. Record it.
                    String owner = logBlockOwnerIds.get(playerId);
                    if( owner == null ) {
                        // TODO: some sort of warning
                        continue;
                    }
                    wcm.setBlockOwner(x,y,z,owner);
                }
                rs.close();
                statement.close();
                con.close();
            }
            catch(SQLException e) {

            }
            currentId = lowestRow;

            // should really be done back on Bukkit sync thread for thread safety
            for(Iterator<Long> i = loadedChunks.iterator(); i.hasNext();) {
                long chunkKey = i.next();
                int chunkX = (int) (chunkKey >> 16);
                int chunkZ = (int) (chunkKey & 0xFFFF);
                if( !bukkitWorld.isChunkLoaded(chunkX, chunkZ) ) {
                    wcm.unloadChunk(chunkX, chunkZ);
                    i.remove();
                }
            }

            syncSave();
        }

        public void syncSave() {
            BukkitTask bukkitTask = plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                public void run() {
                    try {
                        updateWorldId(world, currentId);
                        saveConfig();
                    }
                    catch(Exception e) {
                        log.warn("Caught exception trying to save LogBlock import state", e);
                    }
                }
            });
        }
    }
}
