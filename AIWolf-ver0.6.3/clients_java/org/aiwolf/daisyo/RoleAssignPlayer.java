package org.aiwolf.daisyo;

import org.aiwolf.sample.lib.AbstractRoleAssignPlayer;

public class RoleAssignPlayer extends AbstractRoleAssignPlayer {

	public RoleAssignPlayer() {
		setVillagerPlayer(new VillagerPlayer());
		setSeerPlayer(new SeerPlayer());
		setMediumPlayer(new MediumPlayer());
		setBodyguardPlayer(new BodyguardPlayer());
		setPossessedPlayer(new PossessedPlayer());
		setWerewolfPlayer(new WerewolfPlayer());
	}

	@Override
	public String getName() {
		// TODO 自動生成されたメソッド・スタブ
		return "daisyo";
	}

}
