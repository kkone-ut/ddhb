package camellia.aiwolf.demo;

import org.aiwolf.sample.lib.AbstractRoleAssignPlayer;

public class DemoRoleAssignPlayer extends AbstractRoleAssignPlayer {

	public DemoRoleAssignPlayer() {
		setVillagerPlayer(new CamelliaVillager());
		setSeerPlayer(new CamelliaSeer());
		setWerewolfPlayer(new CamelliaWerewolf());
		setBodyguardPlayer(new CamelliaBodyguard());
		setMediumPlayer(new CamelliaMedium());
		setPossessedPlayer(new CamelliaPossessed());
	}

	@Override
	public String getName() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

}
