package com.bergerkiller.bukkit.tc;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;

import com.bergerkiller.bukkit.config.ConfigurationNode;
import com.bergerkiller.bukkit.config.FileConfiguration;

public class TrainProperties {
	private static HashMap<String, TrainProperties> properties = new HashMap<String, TrainProperties>();
	public static TrainProperties get(String trainname) {
		if (trainname == null || properties == null) return null;
		TrainProperties prop = properties.get(trainname);
		if (prop == null) {
			return new TrainProperties(trainname);
		}
		return prop;
	}
	public static boolean exists(String trainname) {
		if (properties == null) return false;
		return properties.containsKey(trainname);
	}	
	
	private String trainname;	
	public boolean allowLinking = true;
	public boolean trainCollision = true;
	public boolean slowDown = true;
	public boolean pushMobs = false;
	public boolean pushPlayers = false;
	public boolean pushMisc = true;
	public double speedLimit = 0.4;
	public boolean requirePoweredMinecart = false;
	public boolean keepChunksLoaded = false;
	
	private final Set<CartProperties> cartproperties = new HashSet<CartProperties>();
	public Set<CartProperties> getCarts() {
		return this.cartproperties;
	}
	public MinecartGroup getGroup() {
		return MinecartGroup.get(this.trainname);
	}
	
	protected void addCart(MinecartMember member) {
		this.cartproperties.add(member.getProperties());
	}
	protected void removeCart(MinecartMember member) {
		this.cartproperties.remove(member.getProperties());
	}
	
	/*
	 * Owners
	 */
	public boolean hasOwners() {
		for (CartProperties prop : this.cartproperties) {
			if (prop.hasOwners()) return true;
		}
		return false;
	}
	public boolean hasOwnership(Player player) {
        if (!CartProperties.canHaveOwnership(player)) return false;
        if (CartProperties.hasGlobalOwnership(player)) return true;
        if (!this.hasOwners()) return true;
        return this.isOwner(player);
	}
	public boolean isOwner(Player player) {
		for (CartProperties prop : this.cartproperties) {
			if (prop.isOwner(player)) return true;
		}
		return false;
	}	
	public boolean isDirectOwner(Player player) {
		return this.isDirectOwner(player.getName().toLowerCase());
	}
	public boolean isDirectOwner(String player) {
		for (CartProperties prop : this.cartproperties) {
			if (prop.isOwner(player)) return true;
		}
		return false;
	}
	
	/*
	 * Tags
	 */
	public boolean hasTag(String tag) {
		for (CartProperties prop : this.cartproperties) {
			if (prop.hasTag(tag)) return true;
		}
		return false;
	}
	public void clearTags() {
		for (CartProperties prop : this.cartproperties) {
			prop.clearTags();
		}
	}
	public void setTags(String... tags) {
		for (CartProperties prop : this.cartproperties) {
			prop.setTags(tags);
		}
	}
	public void addTags(String... tags) {
		for (CartProperties prop : this.cartproperties) {
			prop.addTags(tags);
		}
	}
	
	/*
	 * Destination
	 */
	public boolean hasDestination() {
		for (CartProperties prop : this.cartproperties) {
			if (prop.hasDestination()) return true;
		}
		return false;
	}
	public String getDestination() {
		for (CartProperties prop : this.cartproperties) {
			if (prop.hasDestination()) return prop.destination;
		}
		return "";
	}
	public void setDestination(String destination) {
		for (CartProperties prop : this.cartproperties) {
			prop.destination = destination;
		}
	}
	public void clearDestination() {
		this.setDestination("");
	}

	/*
	 * Push away settings
	 */
	public boolean canPushAway(Entity entity) {
		if (entity instanceof Player) {
			if (this.pushPlayers) {
				if (!TrainCarts.pushAwayIgnoreOwners) return true;
				if (TrainCarts.pushAwayIgnoreGlobalOwners) {
					if (CartProperties.hasGlobalOwnership((Player) entity)) return false;
				}
				return !this.isOwner((Player) entity);
			}
		} else if (entity instanceof Creature || entity instanceof Slime || entity instanceof Ghast) {
			if (this.pushMobs) return true;
		} else {
			if (this.pushMisc) return true;
		}
		return false;
	}

	/*
	 * Collision settings
	 */
	public boolean canCollide(MinecartMember with) {
		return this.canCollide(with.getGroup());
	}
	public boolean canCollide(MinecartGroup with) {
		return this.trainCollision && with.getProperties().trainCollision;
	}
	public boolean canCollide(Entity with) {
		MinecartMember mm = MinecartMember.get(with);
		if (mm == null) {
			if (this.trainCollision) return true;
			if (with instanceof Player) {
				return this.isOwner((Player) with);
			} else {
				return false;
			}
		} else {
			return this.canCollide(mm);
		}
	}
		
	/*
	 * General coding not related to properties themselves
	 */
	private TrainProperties() {};
	private TrainProperties(String trainname) {
		this.trainname = trainname;
		properties.put(trainname, this);
		this.setDefault();
	}	
	public String getTrainName() {
		return this.trainname;
	}	
	public void remove() {
		properties.remove(this.trainname);
	}
	public void add() {
		properties.put(this.trainname, this);
	}
	public TrainProperties rename(String newtrainname) {
		this.remove();
		this.trainname = newtrainname;
		properties.put(newtrainname, this);
		return this;
	}
	
	public static void init(String filename) {
		load(filename);
	}
	public static void deinit(String filename) {
		save(filename);
		defconfig = null;
		properties.clear();
		properties = null;
	}
    
	/*
	 * Train properties defaults
	 */
	private static FileConfiguration defconfig = null;
	public static void reloadDefaults() {
		defconfig = new FileConfiguration(TrainCarts.plugin, "DefaultTrainProperties.yml");
		defconfig.load();
		defconfig.getNode("default");
		defconfig.getNode("admin");
		defconfig.save();
	}
	public static FileConfiguration getDefaults() {
		if (defconfig == null) reloadDefaults();
		return defconfig;
	}
	public void setDefault() {
		setDefault("default");
	}
	public void setDefault(String key) {
		this.load(getDefaults().getNode(key));
	}
	public void setDefault(Player player) {
		if (player == null) {
			//Set default
			this.setDefault();
		} else {
			//Load it
			for (ConfigurationNode node : getDefaults().getNodes()) {
				if (player.hasPermission("train.properties." + node.getName())) {
					this.load(node);
					break;
				}
			}
		}
	}
	
	/*
	 * Loading and saving
	 */
	public static void load(String filename) {
		FileConfiguration config = new FileConfiguration(new File(filename));
		config.load();
		for (ConfigurationNode node : config.getNodes()) {
			get(node.getName()).load(node);
		}
	}
	public static void save(String filename) {
		FileConfiguration config = new FileConfiguration(new File(filename));
		for (TrainProperties prop : properties.values()) {
			//does this train even exist?!
			if (GroupManager.contains(prop.getTrainName())) {
				ConfigurationNode train = config.getNode(prop.getTrainName());
				prop.save(train);
				if (train.getKeys().isEmpty()) {
					config.set(prop.getTrainName(), null);
				}
			} else {
				config.set(prop.getTrainName(), null);
			}
		}
		config.save();
	}
	public void load(ConfigurationNode node) {
		this.allowLinking = node.get("allowLinking", this.allowLinking);
		this.trainCollision = node.get("trainCollision", this.trainCollision);
		this.slowDown = node.get("slowDown", this.slowDown);
		this.pushMobs = node.get("pushAway.mobs", this.pushMobs);
		this.pushPlayers = node.get("pushAway.players", this.pushPlayers);
		this.pushMisc = node.get("pushAway.misc", this.pushMisc);
		this.speedLimit = Util.limit(node.get("speedLimit", this.speedLimit), 0, 20);
		this.requirePoweredMinecart = node.get("requirePoweredMinecart", this.requirePoweredMinecart);
		this.keepChunksLoaded = node.get("keepChunksLoaded", this.keepChunksLoaded);
		for (ConfigurationNode cart : node.getNode("carts").getNodes()) {
			try {
				CartProperties prop = CartProperties.get(UUID.fromString(cart.getName()));
				this.cartproperties.add(prop);
				prop.load(cart);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	public void load(TrainProperties source) {
		this.allowLinking = source.allowLinking;
		this.trainCollision = source.trainCollision;
		this.slowDown = source.slowDown;
		this.pushMobs = source.pushMobs;
		this.pushPlayers = source.pushPlayers;
		this.pushMisc = source.pushMisc;
		this.speedLimit = Util.limit(source.speedLimit, 0, 20);
		this.requirePoweredMinecart = source.requirePoweredMinecart;
		this.keepChunksLoaded = source.keepChunksLoaded;
		this.cartproperties.clear();
		this.cartproperties.addAll(source.cartproperties);
	}
	public void save(ConfigurationNode node) {		
		node.set("allowLinking", this.allowLinking ? null : false);
		node.set("requirePoweredMinecart", this.requirePoweredMinecart ? true : null);
		node.set("trainCollision", this.trainCollision ? null : false);
		node.set("keepChunksLoaded", this.keepChunksLoaded ? true : null);
		node.set("speedLimit", this.speedLimit != 0.4 ? this.speedLimit : null);
		node.set("slowDown", this.slowDown ? null : false);
		if (this.pushMobs || this.pushPlayers || !this.pushMisc) {
			node.set("pushAway.mobs", this.pushMobs ? true : null);
			node.set("pushAway.players", this.pushPlayers ? true : null);
			node.set("pushAway.misc", this.pushMisc ? null : false);
		} else {
			node.remove("pushAway");
		}
		ConfigurationNode carts = node.getNode("carts");
		for (CartProperties prop : this.cartproperties) {
			ConfigurationNode cart = carts.getNode(prop.getUUID().toString());
			prop.save(cart);
			if (cart.getKeys().isEmpty()) carts.remove(cart.getName());
		}
	}
}
