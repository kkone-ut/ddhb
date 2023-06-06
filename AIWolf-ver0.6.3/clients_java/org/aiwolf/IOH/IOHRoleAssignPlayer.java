package org.aiwolf.IOH;

import org.aiwolf.sample.lib.AbstractRoleAssignPlayer;

public class IOHRoleAssignPlayer extends AbstractRoleAssignPlayer {

	public IOHRoleAssignPlayer() {
		setVillagerPlayer(new IOHVillager());
		setBodyguardPlayer(new IOHBodyguard());
		setMediumPlayer(new IOHMedium());
		setSeerPlayer(new IOHSeer());
		setPossessedPlayer(new IOHPossessed());
		setWerewolfPlayer(new IOHWerewolf());
	} 

	@Override
	public String getName() {
		return "ioh";
	}
}
