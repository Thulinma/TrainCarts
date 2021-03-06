package com.bergerkiller.bukkit.tc.signactions;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.material.Rails;

import com.bergerkiller.bukkit.tc.MinecartGroup;
import com.bergerkiller.bukkit.tc.MinecartMember;
import com.bergerkiller.bukkit.tc.StationMode;
import com.bergerkiller.bukkit.tc.TrainCarts;
import com.bergerkiller.bukkit.tc.API.SignActionEvent;
import com.bergerkiller.bukkit.tc.actions.BlockActionSetLevers;
import com.bergerkiller.bukkit.tc.permissions.Permission;
import com.bergerkiller.bukkit.tc.utils.BlockUtil;
import com.bergerkiller.bukkit.tc.utils.FaceUtil;

public class SignActionStation extends SignAction {
	
	@Override
	public void execute(SignActionEvent info) {
		if (info.isAction(SignActionType.REDSTONE_CHANGE, SignActionType.GROUP_ENTER, SignActionType.GROUP_LEAVE)) {
			if (info.isTrainSign() || info.isCartSign()) {
				if (info.isType("station")) {
					if (info.hasRails()) {
						MinecartGroup group = info.getGroup();
						if (group != null) {
							if (info.isAction(SignActionType.GROUP_LEAVE)) {
							} else if (!info.isPowered()) {
								group.clearActions();
							} else {
								//Check if not already targeting
								if (group != null && info.hasRails()) {		
									//Get station length
									double length = 0;
									try {
										length = Double.parseDouble(info.getLine(1).substring(7).trim());
									} catch (Exception ex) {};
									long delayMS = 0;
									try {
										delayMS = (long) (Double.parseDouble(info.getLine(2)) * 1000);
									} catch (Exception ex) {};
									//Get the mode used
                                    StationMode mode = StationMode.fromString(info.getLine(3));
	
									//Get the middle minecart
									MinecartMember midd = group.middle();
									//First, get the direction of the tracks above
									BlockFace dir = info.getRailDirection();
									//Get the length of the track to center in
									if (length == 0) {
										//manually calculate the length
										//use the amount of straight blocks
										for (BlockFace face : FaceUtil.getFaces(dir)) {
											int tlength = 0;
											//get the type of rail required
											BlockFace checkface = face;
											if (checkface == BlockFace.NORTH)
												checkface = BlockFace.SOUTH;
											if (checkface == BlockFace.EAST)
												checkface = BlockFace.WEST;

											Block b = info.getRails();
											int maxlength = 20;
											while (true) {
												//Next until invalid
												b = b.getRelative(face);
												Rails rr = BlockUtil.getRails(b);
												if (rr == null || rr.getDirection() != checkface)
													break;
												tlength++;

												//prevent inf. loop or long processing
												maxlength--;
												if (maxlength <= 0) break;
											}
											//Update the length
											if (length == 0 || tlength < length) length = tlength;
										}
									}
									boolean west = info.isPowered(BlockFace.WEST);
									boolean east = info.isPowered(BlockFace.EAST);
									boolean north = info.isPowered(BlockFace.NORTH);
									boolean south = info.isPowered(BlockFace.SOUTH);

									//which directions to move, or brake?
									BlockFace instruction = BlockFace.UP; //SELF is brake
									if (dir == BlockFace.WEST) {
										if (west && !east) {
											instruction = BlockFace.WEST;
										} else if (east && !west) {
											instruction = BlockFace.EAST;
										} else {
											instruction = BlockFace.SELF;
										}
									} else if (dir == BlockFace.SOUTH) {
										if (north && !south) {
											instruction = BlockFace.NORTH;
										} else if (south && !north) {
											instruction = BlockFace.SOUTH;
										} else {
											instruction = BlockFace.SELF;
										}
									}
									if (instruction == BlockFace.UP) return; 

									//What do we do?
									if (instruction == BlockFace.SELF) {
										if (north || east || south || west) {
											//Redstone change and moving?
											if (!info.isAction(SignActionType.REDSTONE_CHANGE) || !info.getMember().isMoving()) {
												//Brake
												//TODO: ADD CHECK?!
												group.clearActions();		
												midd.addActionLaunch(info.getRailLocation(), 0);
												BlockFace trainDirection = null;
												if (mode == StationMode.CONTINUE) {
													trainDirection = midd.getDirection();
												} else if (mode == StationMode.REVERSE) {
													trainDirection = midd.getDirection().getOppositeFace();
												} else if (mode == StationMode.LEFT || mode == StationMode.RIGHT) {
													trainDirection = info.getFacing();
													//Convert
													if (mode == StationMode.LEFT) {
														trainDirection = FaceUtil.rotate(trainDirection, 2);
													} else {
														trainDirection = FaceUtil.rotate(trainDirection, -2);
													}	
												}
												if (trainDirection != null) {
													//Actual launching here
													if (delayMS > 0) {
														if (TrainCarts.playSoundAtStation) group.addActionSizzle();
														info.getGroup().addAction(new BlockActionSetLevers(info.getAttachedBlock(), true));
													}
													group.addActionWait(delayMS);
													midd.addActionLaunch(trainDirection, length, TrainCarts.launchForce);
												} else {
													group.addActionWaitForever();
													if (TrainCarts.playSoundAtStation) group.addActionSizzle();
													info.getGroup().addAction(new BlockActionSetLevers(info.getAttachedBlock(), true));
												}
											}
										}
									} else {
										//Launch
										group.clearActions();
										MinecartMember head = group.head();

										if (delayMS > 0 || (head.isMoving() && head.getDirection() != instruction)) {
											//Reversing or has delay, need to center it in the middle first
											midd.addActionLaunch(info.getRailLocation(), 0);
										}
										if (delayMS > 0) {
											if (TrainCarts.playSoundAtStation) group.addActionSizzle();
											info.getGroup().addAction(new BlockActionSetLevers(info.getAttachedBlock(), true));
										}
										group.addActionWait(delayMS);
										midd.addActionLaunch(instruction, length, TrainCarts.launchForce);
									}
								}
							}
						}
						if (info.isAction(SignActionType.GROUP_LEAVE)) {
							info.setLevers(false);
						}
					}
				}
			}
		}
	}

	@Override
	public void build(SignChangeEvent event, String type, SignActionMode mode) {
	    if (mode != SignActionMode.NONE) {
	    	if (type.startsWith("station")) {
	    		handleBuild(event, Permission.BUILD_STATION, "station", "stop, wait and launch trains");
	    	}
	    }
	}
}
