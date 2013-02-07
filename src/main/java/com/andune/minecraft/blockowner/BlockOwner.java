package com.andune.minecraft.blockowner;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.andune.minecraft.blockowner.util.Debug;
import com.andune.minecraft.blockowner.util.JarUtils;

public class BlockOwner extends JavaPlugin
{
	private static final Logger log = Logger.getLogger(BlockOwner.class.toString());
	private static final String logPrefix = "[BlockOwner] ";
	
	private Debug debug;
	private JarUtils jarUtils;
	private int buildNumber = -1;
	private GlobalChunkManager globalChunkManager;
	
	@Override
	public void onEnable() {
    	Debug.getInstance().init(log, logPrefix, "plugins/BlockOwner/debug.log", false);
		debug = Debug.getInstance();
    	jarUtils = new JarUtils(this, getFile(), log, logPrefix);
		buildNumber = jarUtils.getBuildNumber();
		Debug.getInstance().setDebug(getConfig().getBoolean("debug", false));
		
		Owners owners = new Owners(new YamlConfiguration(), new File(getDataFolder(), "owners.yml"));
		try {
			owners.loadConfig();
		}
		catch(Exception e) {
			log.log(Level.SEVERE, "Error loading owners: "+e.getMessage(), e);
		}
		
		globalChunkManager = new GlobalChunkManager(new FileChunkStorage(this), owners);
		getServer().getPluginManager().registerEvents(globalChunkManager, this);
		getServer().getPluginManager().registerEvents(new BlockListener(globalChunkManager), this);
		getServer().getPluginManager().registerEvents(new PlayerListener(globalChunkManager), this);
		
		info("version "+getDescription().getVersion()+", build "+buildNumber+" is enabled");
	}
	
	@Override
	public void onDisable() {
		info("version "+getDescription().getVersion()+", build "+buildNumber+" is disabled");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
	    if( "bosave".equalsIgnoreCase(label) ) {
	        globalChunkManager.saveAll();
            sender.sendMessage("All chunks saved");
	    }
	    else if( "boq".equalsIgnoreCase(label) ) {
	        if( sender instanceof Player ) {
	            Player p = (Player) sender;
	            List<Block> los = p.getLineOfSight(null, 100);
	            if( los != null ) {
	                Block b = null;
	                for(int i=0; i < 99; i++) {
	                    b = los.get(i);
	                    if( !b.isEmpty() )
	                        break;
	                }
	                if( b != null ) {
    	                String owner = globalChunkManager.getBlockOwner(b);
    	                sender.sendMessage("Block owner for block "+b+" is "+owner);
	                }
	            }
	        }
	    }
	    else if( "boa".equalsIgnoreCase(label) ) {		// area report
	        if( sender instanceof Player ) {
	            final Player p = (Player) sender;
	            final Location l = p.getLocation();
	            final World world = l.getWorld();
	            final String worldName = world.getName();
	            final int playerX = (int) l.getX();
	            final int playerY = (int) l.getY();
	            final int playerZ = (int) l.getZ();
	            final int radius = 10;
	            
	            long startTime = System.currentTimeMillis();
	            int blocksChecked=0;
	        	final HashMap<String, Integer> ownerCountMap = new HashMap<String, Integer>();
	            for(int x=playerX-radius; x <= playerX+radius; x++) {
		            for(int y=playerY-radius; y <= playerY+radius; y++) {
			            for(int z=playerZ-radius; z <= playerZ+radius; z++) {
			            	blocksChecked++;
			            	// fast skip air blocks, they have no owner
			            	if( world.getBlockTypeIdAt(x,y,z) == 0 )
			            		continue;

			            	String owner = globalChunkManager.getBlockOwner(worldName, x, y, z);
			            	if( owner != null ) {
			            		Integer i = ownerCountMap.get(owner);
			            		if( i == null )
			            			i = Integer.valueOf(1);
			            		else
			            			i++;
			            		ownerCountMap.put(owner, i);
			            	}
			            }
		            }
	            }
	            
	            // sort the list
	            TreeMap<Integer, ArrayList<String>> sortedMap = new TreeMap<Integer, ArrayList<String>>(new Comparator<Integer>() {
					public int compare(Integer o1, Integer o2) {
						return o2.compareTo(o1);	// ascending order
					}
				});
	            for(Entry<String, Integer> entry : ownerCountMap.entrySet()) {
	            	ArrayList<String> list = sortedMap.get(entry.getValue());
	            	if( list == null ) {
	            		list = new ArrayList<String>();
	            		sortedMap.put(entry.getValue(), list);
	            	}
	            	list.add(entry.getKey());
	            }
	            
	            int i=0;
	            sender.sendMessage("Top 10 Block owner report for radius "+radius);
	            for(Entry<Integer, ArrayList<String>> entry : sortedMap.entrySet()) {
	            	StringBuffer sb = new StringBuffer("Owner(s) with "+entry.getKey()+" blocks: ");
	            	for(String owner : entry.getValue()) {
	            		sb.append(owner);
	            		sb.append(",");
	            	}
	            	sender.sendMessage(sb.substring(0, sb.length()-1));
	            	
	            	// we only show the top 10 entries
	            	if( ++i >= 10 )
	            		break;
	            }
	            long endTime = System.currentTimeMillis();
	            Debug.getInstance().debug("/boa total time running = "+(endTime-startTime)+"ms (total blocks checked "+blocksChecked+")");
	            sender.sendMessage("/boa total time running = "+(endTime-startTime)+"ms (total blocks checked "+blocksChecked+")");
	        }
	    }
	    return true;
	}
	
	/** Log a message.
	 * 
	 * @param msg
	 */
	public void info(String msg) {
		log.info(logPrefix+msg);
	}
	
	public void debug(Object...args) {
		debug.debug(args);
	}
}
