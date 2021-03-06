package com.bergerkiller.bukkit.tc.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.tc.Destinations;
import com.bergerkiller.bukkit.tc.GroupManager;
import com.bergerkiller.bukkit.tc.permissions.NoPermissionException;
import com.bergerkiller.bukkit.tc.permissions.Permission;

public class GlobalCommands {
	
	public static boolean execute(CommandSender sender, String[] args) throws NoPermissionException {
		if (args[0].equals("removeall") || args[0].equals("destroyall")) {
			Permission.COMMAND_DESTROYALL.handle(sender);
			if (args.length == 2) {
				String cname = args[1].toLowerCase();
				World w = null;
				for (World world : Bukkit.getServer().getWorlds()) {
					String wname = world.getName().toLowerCase();
					if (wname.equals(cname)) {
						w = world;
						break;
					}
				}
				if (w == null) {
					for (World world : Bukkit.getServer().getWorlds()) {
						String wname = world.getName().toLowerCase();
						if (wname.contains(cname)) {
							w = world;
							break;
						}
					}
				}
				if (w != null) {
					int count = GroupManager.destroyAll(w);
					sender.sendMessage(ChatColor.RED.toString() + count + " (visible) trains have been destroyed!");	
				} else {
					sender.sendMessage(ChatColor.RED + "World not found!");
				}
			} else {
				int count = GroupManager.destroyAll();
				sender.sendMessage(ChatColor.RED.toString() + count + " (visible) trains have been destroyed!");	
			}
			return true;
		} else if (args[0].equals("reroute")) {
			Permission.COMMAND_REROUTE.handle(sender);
			Destinations.clear();
			sender.sendMessage("All train routings will be recalculated.");
			return true;
		}
		return false;
	}

}
