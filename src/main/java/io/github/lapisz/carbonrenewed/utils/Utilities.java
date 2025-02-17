package io.github.lapisz.carbonrenewed.utils;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;

import io.github.lapisz.carbonrenewed.CarbonRenewed;
import io.github.lapisz.carbonrenewed.DynamicEnumType;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.NetworkManager;
import net.minecraft.server.v1_16_R3.Packet;
import net.minecraft.server.v1_16_R3.Vec3D;

/**
 *
 * @author Navid
 */
public class Utilities {

    private static CarbonRenewed plugin = null;
    public final static SecureRandom random = new SecureRandom();
    
    private Utilities() {
        throw new UnsupportedOperationException("No, bad!");
    }

    /**
     * Can only be instantiated once.
     * @param instance
     */
    public static void instantiate(CarbonRenewed instance) {
        if (plugin == null) {
            plugin = instance;
        }
    }

    /**
     * Registers a bukkit command without the need for a plugin.yml entry.
     *
     * Yes, I'm terrible.
     * @param fallbackPrefix
     * @param cmd
     */
    public static void registerBukkitCommand(String fallbackPrefix, Command cmd) {
        try {
        if (Bukkit.getServer() instanceof CraftServer) {
            Field f = CraftServer.class.getDeclaredField("commandMap");
            f.setAccessible(true);
            CommandMap cmap = (CommandMap) f.get(Bukkit.getServer());
            cmap.register(fallbackPrefix, cmd);
        }
        } catch (NoSuchFieldException e) {
            e.printStackTrace(System.out);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Utilities.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (IllegalAccessException ex) {
            Logger.getLogger(Utilities.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
	 * Method added to clean up some reflection code
	 * Named setField2 not to confuse with the setField in DynamicEnumType
	 * To do: figure out an alternative way for Java 16 for removeFinalModifier
	 * @param <T>
	 * @param clazz               the class of the field you want to access (eg Material.class)
	 * @param fieldName           getDeclaredField name of the field you want to access
	 * @param instance            instance for field.set, can be null if static
	 * @param fieldValue          the new value of what you want to set the field to
	 * @param removeFinalModifier this needs to be true if the field you want to modify has a final modifier/keyword
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static <T> void setField2(Class<T> clazz, String fieldName, T instance, Object fieldValue, boolean removeFinalModifier)
	throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
	{
		Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		
		if(removeFinalModifier) {
			Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
		}
		
		field.set(instance, fieldValue);
	}
	
    /**
     * Returns the BUKKIT ENTITYTYPE.
     * @param name
     * @param id
     * @param entityClass
     * @return
     */
    @SuppressWarnings("unchecked")
    public static EntityType addEntity(String name, int id, Class<? extends Entity> entityClass) {
        EntityType entityType = DynamicEnumType.addEnum(EntityType.class, name, new Class[] {String.class, entityClass.getClass(), Integer.TYPE}, new Object[] {name, entityClass.getClass(), id});
        try {
                Field field = EntityType.class.getDeclaredField("NAME_MAP");
                field.setAccessible(true);
                Object object = field.get(null);
                Map<String, EntityType> NAME_MAP = (Map<String, EntityType>) object;
                NAME_MAP.put(name, entityType);
                if (plugin.getConfig().getBoolean("debug.verbose", false))
                    CarbonRenewed.log.log(Level.INFO, "[Carbon] Entity {0} with ID {1} was injected into CraftBukkit.", new Object[]{name, id});
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace(System.out);
        }
        try {
                Field field = EntityType.class.getDeclaredField("ID_MAP");
                field.setAccessible(true);
                Object object = field.get(null);
                Map<Short, EntityType> ID_MAP = (Map<Short, EntityType>) object;
                ID_MAP.put((short)id, entityType);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace(System.out);
        }
        return entityType;
    }

    /**
     * Injects a material to Bukkit given a namespace, name, id, and max stack size.
     * See org.bukkit.Material for id's that are currently in use.
     * @param namespace     namespace of the item that the material is to be linked to (eg "minecraft" in minecraft:emerald). all lowercase
     * @param itemName      name of the material. should be same as the item you want it to correspond to, but all uppercase instead.
     * @param indexId       index id to register the material in. can be any, as long as it doesnt conflict with the stuff already registered or with the stuff in bukkit material class
     * @param maxStackSize  self explanatory
     * @return
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    public static Material addMaterial(String namespace, String materialName, int indexId, int maxStackSize) {
    	//Inject to enum and set all the properties
    	
    	//Add the item name to the enum (eg Material.DIAMOND)
        Material material = DynamicEnumType.addEnum(Material.class, materialName, new Class[] { Integer.TYPE }, new Object[] { indexId });
        try {
                Field field = Material.class.getDeclaredField("BY_NAME");
                field.setAccessible(true);
                Object object = field.get(null);
                Map<String, Material> BY_NAME = (Map<String, Material>) object;
                BY_NAME.put(materialName, material);
                if (plugin.getConfig().getBoolean("debug.verbose", false))
                	CarbonRenewed.log.log(Level.INFO, CarbonRenewed.PREFIX + "Injected material {0}''s name.", materialName);
                
                //Add the item index id, just use one that isnt occupied
                setField2(Material.class, "id", material, indexId, true);
        		if (plugin.getConfig().getBoolean("debug.verbose", false))
        			CarbonRenewed.log.log(Level.INFO, CarbonRenewed.PREFIX + "Injected material {0}''s ID {1}.", new Object[]{materialName, Integer.toString(indexId)});
        
        		//Set the item key because it defaults to minecraft:itemname
				NamespacedKey newKey = new NamespacedKey(namespace, materialName.toLowerCase(Locale.ROOT));
				
        		setField2(Material.class, "key", material, newKey, true);
        		if (plugin.getConfig().getBoolean("debug.verbose", false))
        			CarbonRenewed.log.log(Level.INFO, CarbonRenewed.PREFIX + "Injected material {0}''s key {1}.", new Object[]{materialName, newKey.toString()});
        
        		//Set the item max stack size
        		setField2(Material.class, "maxStack", material, maxStackSize, true);
        		if (plugin.getConfig().getBoolean("debug.verbose", false))
        			CarbonRenewed.log.log(Level.INFO, CarbonRenewed.PREFIX + "Injected material {0}''s maxStackSize {1}.", new Object[]{materialName, Integer.toString(maxStackSize)});
        		
        		//set durability (should always be 0 post-flattening)
        		setField2(Material.class, "durability", material, (short) 0, true);
        		if(plugin.getConfig().getBoolean("debug.superverbose", false))
        			CarbonRenewed.log.log(Level.INFO, CarbonRenewed.PREFIX + "SUPERVERBOSE: Successfully set Material.durability of {0}", materialName);
        		
        		/*set data class (default is MaterialData.class)
        		 * to do: make the class adjustable for more advanced things later 
        		 */
        		//although this is public it is final so still need to use reflection
        		setField2(Material.class, "data", material, MaterialData.class, true);
        		if(plugin.getConfig().getBoolean("debug.superverbose", false))
        			CarbonRenewed.log.log(Level.INFO, CarbonRenewed.PREFIX + "SUPERVERBOSE: Successfully set Material.data of {0}", materialName);
        		
        		//indicate that this is not a legacy material
        		setField2(Material.class, "legacy", material, false, true);
        		if(plugin.getConfig().getBoolean("debug.superverbose", false))
        			CarbonRenewed.log.log(Level.INFO, CarbonRenewed.PREFIX + "SUPERVERBOSE: Successfully set Material.legacy of {0}", materialName);
        		
        		//Bukkit Material class code for caching constructor - gnu gplv3
        		if(MaterialData.class.isAssignableFrom(material.data)) {
        			Constructor<MaterialData> newCtor = (Constructor<MaterialData>) material.data.getConstructor(Material.class, byte.class);
        			
        			setField2(Material.class, "ctor", material, newCtor, true);
        			if(plugin.getConfig().getBoolean("debug.superverbose", false))
            			CarbonRenewed.log.log(Level.INFO, CarbonRenewed.PREFIX + "SUPERVERBOSE: Successfully set Material.ctor of {0} to newCtor", materialName);
        		} else {
        			setField2(Material.class, "ctor", material, null, true);
        			if(plugin.getConfig().getBoolean("debug.superverbose", false))
            			CarbonRenewed.log.log(Level.INFO, CarbonRenewed.PREFIX + "SUPERVERBOSE: Successfully set Material.ctor of {0} to null", materialName);
        		}
        		
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException | NoSuchMethodException e) {
                e.printStackTrace(System.out);
        }
        return material;
    }

    /**
     * Similar to addMaterial(String name, int id), but used to add materials with more special properties
     * Do not use, as it has not been ported to 1.16 yet. 
     * @param name
     * @param id
     * @param data
     * @return
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static Material addMaterial(String name, int id, short data) {
        Material material = DynamicEnumType.addEnum(Material.class, name, new Class[] { Integer.TYPE }, new Object[] { id });
        try {
                Field field = Material.class.getDeclaredField("BY_NAME");
                field.setAccessible(true);
                Object object = field.get(null);
                Map<String, Material> BY_NAME = (Map<String, Material>) object;
                BY_NAME.put(name, material);
                if (plugin.getConfig().getBoolean("debug.verbose", false))
                	CarbonRenewed.log.log(Level.INFO, "[Carbon] Material {0} with ID {1} with data {2} was injected into CraftBukkit.", new Object[]{name, id, data});
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e)
        {
                e.printStackTrace(System.out);
        }
        try {
                Field field = Material.class.getDeclaredField("byId");
                field.setAccessible(true);
                Object object = field.get(0);
                Material[] byId = (Material[]) object;
                byId[id] = material;
                field.set(object, byId);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace(System.out);
        }
        try {
                Field field = Material.class.getDeclaredField("durability");
                field.setAccessible(true);
                Object object = field.get((short)0);
                Material[] durability = (Material[]) object;
                durability[data] = material;
                field.set(object, durability);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace(System.out);
        }
        return material;
    }


    /**
     * Returns 0 if there's an error accessing the "strength" field in the minecraft server
     * Blocks class, otherwise, returns the block's given strength.
     * @param b
     * @return
     */
    public static float getBlockStrength(net.minecraft.server.v1_16_R3.Block b) {
        try {
            Field field = b.getClass().getField("strength");
            field.setAccessible(true);
            return field.getFloat(b);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            Logger.getLogger(Utilities.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0;
    }

    /**
     * Returns 0 if there's an error accessing the "durability" field in the minecraft server
     * Blocks class, otherwise, returns the block's given strength.
     * @param b
     * @return
     */
    public static float getBlockDurability(net.minecraft.server.v1_16_R3.Block b) {
        try {
            Field field = b.getClass().getField("durability");
            field.setAccessible(true);
            return field.getFloat(b);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            Logger.getLogger(Utilities.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0;
    }


    /**
     * Returns all adjacent blocks to a specified block.
     * Returns an empty list if none were found.
     * Air is not included.
     * @param source
     * @return
     */
    public static List<Block> getAllAdjacentBlocks(Block source) {
        List<Block> list = new ArrayList<Block>();
        for (BlockFace f : BlockFace.values()) {
            Block rel = source.getRelative(f);
            if (rel.getType() != Material.AIR) {
            list.add(rel);
            }
        }
        return list;
    }

    public static final int CLIENT_1_16_PROTOCOL_VERSION = 735;
    public static final int CLIENT_1_16_1_PROTOCOL_VERSION = 736;
    public static final int CLIENT_1_16_2_PROTOCOL_VERSION = 751;
    public static final int CLIENT_1_16_3_PROTOCOL_VERSION = 753;
    public static final int CLIENT_1_16_4_1_16_5_PROTOCOL_VERSION = 754;
    
    /**
     * Gets protocol version of a player. Returns 0 if method failed somehow
     * @param player
     * @return
     */
    public static int getProtocolVersion(Player player) {
    	int protocolVersion = 0;
    	NetworkManager manager = ((CraftPlayer) player).getHandle().playerConnection.networkManager;
    	
    	//Attempt a reflection to grab the protocol version of the player
    	try {
    		Field field = manager.getClass().getDeclaredField("protocolVersion");
    		field.setAccessible(true);
    		protocolVersion = field.getInt(manager);
    		
    	} catch (NoSuchFieldException | IllegalAccessException | SecurityException e) {
    		e.printStackTrace(); 
    		
    	}
    	
    	return protocolVersion;
    }
    
    /*
    private static int getProtocolVersion(EntityPlayer nmsPlayer) {
    	return nmsPlayer.playerConnection.networkManager.getVersion();
    } */
    
    /**
     * Sends packet to a player
     * @param player
     * @param packet
     */
    @SuppressWarnings("rawtypes")
    public static void sendPacket(Player player, Packet packet) {
    	EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
    	nmsPlayer.playerConnection.sendPacket(packet);
    }

    /**
     * Returns the squared distance to the location.
     * @param entity
     * @param target
     * @return
     */
    public static double getDistanceSqTo(net.minecraft.server.v1_16_R3.Entity entity, double x, double y, double z) {
        double diffX = entity.locX() - x;
        double diffY = entity.locY() - y;
        double diffZ = entity.locZ() - z;
        return diffX * diffX + diffY * diffY + diffZ * diffZ;
    }

    /**
     * Returns the squared distance to the vec.
     * @param entity
     * @param target
     * @return
     */
    public static double getDistanceSqToVec(net.minecraft.server.v1_16_R3.Entity entity, Vec3D target) {
        double diffX = entity.locX() - target.getX();
        double diffY = entity.locY() - target.getY();
        double diffZ = entity.locZ() - target.getZ();
        return diffX * diffX + diffY * diffY + diffZ * diffZ;
    }

    /**
     * Returns the squared distance to the entity.
     * @param entity
     * @param target
     * @return
     */
    public static double getDistanceSqToEntity(net.minecraft.server.v1_16_R3.Entity entity, net.minecraft.server.v1_16_R3.Entity target) {
        double diffX = entity.locX() - target.locX();
        double diffY = entity.locY() - target.locY();
        double diffZ = entity.locZ() - target.locZ();
        return diffX * diffX + diffY * diffY + diffZ * diffZ;
    }

    /**
     * Sets accessibleobject accessible state an returns this object
     * @param object
     * @param accessible
     * @return
     */
    @SuppressWarnings("unchecked")
	public static <T extends AccessibleObject> T setAccessible(Class<T> objectType, AccessibleObject object, boolean accessible) {
    	object.setAccessible(accessible);
    	return (T) object;
    }
        
    /**
     * Grabs the numerical version portion of the full server's version.
     * @param serverVersion
     * @return 
     */
    public static String getMinecraftVersion(String serverVersion) {
        Pattern pattern = Pattern.compile("\\(.*?\\) ?");
        Matcher matcher = pattern.matcher(serverVersion);
        String regex = "";
        if (matcher.find()) {
            regex = matcher.group(0);
        }
        regex = regex.replaceAll("\\([M][C][:][\" \"]", "").replace(')', ' ').trim();
        return regex;
    }

}
