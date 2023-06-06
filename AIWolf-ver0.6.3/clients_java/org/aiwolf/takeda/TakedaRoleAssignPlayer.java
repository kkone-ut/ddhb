package org.aiwolf.takeda;

import org.aiwolf.sample.lib.AbstractRoleAssignPlayer;

public class TakedaRoleAssignPlayer extends AbstractRoleAssignPlayer {

	
	public TakedaRoleAssignPlayer() {
		
		setVillagerPlayer(new TakedaVillager());
		setBodyguardPlayer(new TakedaBodyguard());
		setMediumPlayer(new TakedaMedium());
		setSeerPlayer(new TakedaSeer());
		setPossessedPlayer(new TakedaPossessed());
		setWerewolfPlayer(new TakedaWerewolf());
	}
	
	@Override
	public String getName() {
		return "takeda";
	}

}
