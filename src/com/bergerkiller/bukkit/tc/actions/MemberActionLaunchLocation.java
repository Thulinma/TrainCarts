package com.bergerkiller.bukkit.tc.actions;

import org.bukkit.Location;

import com.bergerkiller.bukkit.tc.MinecartMember;
import com.bergerkiller.bukkit.tc.utils.FaceUtil;

public class MemberActionLaunchLocation extends MemberActionLaunchDirection implements VelocityAction {

	private final Location target;
	public MemberActionLaunchLocation(final MinecartMember member, double targetvelocity, Location target) {
		super(member, member.distance(target), targetvelocity, member.getDirection());
		this.target = target.clone();
	}
		
	public void start() {
		//update direction to launch at
		super.setDirection(FaceUtil.getDirection(this.getMember().getLocation(), this.target, false));
		super.setTargetDistance(this.getMember().distance(this.target));
		super.start();
	}
	
}
