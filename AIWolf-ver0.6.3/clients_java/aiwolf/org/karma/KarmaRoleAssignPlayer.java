package aiwolf.org.karma;

import org.aiwolf.sample.lib.AbstractRoleAssignPlayer;

public class KarmaRoleAssignPlayer extends AbstractRoleAssignPlayer {

	
	public KarmaRoleAssignPlayer() {
		
		setVillagerPlayer(new KarmaVillager());
		setBodyguardPlayer(new KarmaBodyguard());
		setMediumPlayer(new KarmaMedium());
		setSeerPlayer(new KarmaSeer());
		setPossessedPlayer(new KarmaPossessed());
		setWerewolfPlayer(new KarmaWerewolf());
	}
	
	@Override
	public String getName() {
		return "karma";
	}

}
