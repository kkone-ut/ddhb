package org.aiwolf.TOKU;

import org.aiwolf.sample.lib.AbstractRoleAssignPlayer;

public class TOKURoleAssginPlayer extends AbstractRoleAssignPlayer {

	public TOKURoleAssginPlayer() {

		setVillagerPlayer(new TOKUVillager());
		setBodyguardPlayer(new TOKUBodyguard());
		setMediumPlayer(new TOKUMedium());
		setSeerPlayer(new TOKUSeer());
		setPossessedPlayer(new TOKUPossessed());
		setWerewolfPlayer(new TOKUWerewolf());
	}

	@Override
	public String getName() {
		return "takeda";
	}

}
